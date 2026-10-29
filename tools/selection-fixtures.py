#!/usr/bin/env python3
"""Semantic-only prototype configurations and external provider fixtures. Never production guidance."""
import importlib.util,json,pathlib,sys
spec=importlib.util.spec_from_file_location('fixtures',pathlib.Path(__file__).with_name('behavior-fixtures.py'));f=importlib.util.module_from_spec(spec);spec.loader.exec_module(f)
L=lambda x:dict(value=x)
E=lambda op,*args:dict(op=op,args=list(args))
B=lambda module,function:dict(module=module,function=function)
I=lambda id,label,type,value,minimum=0,maximum=100000:dict(id=id,label=label,type=type,value=value,minimum=minimum,maximum=maximum)
R=lambda id,binding,*arguments:dict(id=id,binding=binding,arguments=list(arguments))
Q=lambda minimum,maximum,predicate:dict(minimum=minimum,maximum=maximum,predicate=predicate)
EMPTY=dict(path='',id='',quantity='')
def food():
 return dict(title='Crew meals',inputs=[I('total','Required quantity','integer',6,0,100),I('budget','Budget','money',8000),I('deadline','Required by','time',1110,0,1439)],resources=[R('menu',B('host/pizza','menu')),R('windows',B('host/pizza','delivery'),E('clock','$deadline'))],selection=dict(source='$menu',id='id',label='name',cost='$row.cents',capacity='$total',eligible=E('eq','$row.diameterMm',L(300)),budget=E('subtract','$budget',E('first',E('filter','$windows','$row.meetsRequirement'))),quotas=[Q('$total','$total',L(True)),Q(L(2),'$total','$row.vegan')],components=EMPTY),evaluation=dict(binding={},arguments=[],cost='$candidate.cost',eligible=L(True),ttlMillis=L(60000)))

def equipment():
 return dict(title='Rental comparison',inputs=[I('start','Start','text','2026-10-23T14:15:00Z'),I('end','End','text','2026-10-23T20:30:00Z')],resources=[R('catalogue',B('host/rental','equipment'),'$start','$end')],selection=dict(source='$catalogue',id='id',label='name',cost=L(0),capacity='$row.available',eligible=E('ge','$row.available',L(1)),budget=L(0),quotas=[Q(L(1),L(1),E('and',E('eq','$row.attributes.kind',L('stand')),E('and','$row.attributes.adjustable','$row.attributes.clipAdapter'))),Q(L(1),L(1),E('and',E('eq','$row.attributes.kind',L('cable')),E('and','$row.attributes.balanced',E('ge','$row.attributes.lengthMm',L(10000)))))],components=dict(path='components',id='itemId',quantity='quantity')),evaluation=dict(binding=B('host/rental','quote'),arguments=[dict(basket='$candidate.items',encoding='idQuantity',idField='',quantityField=''),'$start','$end'],cost='$reply.cents',eligible='$reply.available',ttlMillis=E('multiply','$reply.expiresInMinutes',L(60000))))

def laboratory():
 return dict(title='Lab station',inputs=[I('zone','Zone','text','north'),I('headcount','Stations','integer',1,1,10)],resources=[R('inventory',B('host/laboratory','inventory'),'$zone',L(True))],selection=dict(source='$inventory',id='sku',label='caption',cost=L(0),capacity='$row.stock',eligible=E('ge','$row.stock',L(1)),budget=L(0),quotas=[Q('$headcount','$headcount',E('eq','$row.spec.role',L(role))) for role in ['sensor','power','logger']],components=dict(path='contents',id='skuRef',quantity='units')),evaluation=dict(binding=B('host/laboratory','assess'),arguments=['$zone',dict(basket='$candidate.items',encoding='records',idField='sku',quantityField='units'),L(7)],cost='$reply.charge',eligible='$reply.ok',ttlMillis='$reply.validForMs'))

def contracts():
 # This isolated prototype has a different laboratory signature from composed fixtures.
 cs=[c for c in f.contracts() if c['module']!='host/laboratory']
 def ty(kind,**kw):return dict(kind=kind,fields=kw.pop('fields',[]),element=kw.pop('element',None),maximum=128,minimumNumber=-1e12,maximumNumber=1e12,**kw)
 n=ty('number');s=ty('string');b=ty('boolean')
 def rec(**fields):return ty('record',fields=[dict(name=name,type=value,optional=False) for name,value in fields.items()])
 def arr(t):return ty('array',element=t)
 def fn(name,result,**params):return dict(name=name,description='External test boundary',parameters=[dict(name=k,type=v) for k,v in params.items()],result=result)
 cs.append(dict(version=1,module='host/laboratory',description='Unrelated provider test',functions=[fn('inventory',arr(rec(sku=s,caption=s,stock=n,spec=rec(role=s),contents=arr(rec(skuRef=s,units=n)))),zone=s,availableOnly=b),fn('assess',rec(charge=n,ok=b,validForMs=n),zone=s,items=arr(rec(sku=s,units=n)),revision=n)]))
 return cs

if __name__=='__main__':
 out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
 for name,fn in [('food',food),('equipment',equipment),('laboratory',laboratory)]:
  p=fn()
  if name=='food':
   fee=dict(field='deliveryCents',of=E('first',E('filter','$windows','$row.meetsRequirement')))
   p['selection']['budget']=E('subtract','$budget',fee)
   p['evaluation']['cost']=E('add','$candidate.cost',fee)
   p['evaluation']['eligible']=E('gt',E('length',E('filter','$windows','$row.meetsRequirement')),L(0))
  (out/(name+'.json')).write_text(json.dumps(p,indent=2))
 (out/'contracts.json').write_text(json.dumps(contracts(),indent=2))
