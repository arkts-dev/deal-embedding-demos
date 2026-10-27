#!/usr/bin/env python3
"""Typed behavioral plans for compiler/runtime tests only. Never synthesis instructions."""
import json
import pathlib

class Plan:
    def __init__(self):
        self.p=dict(records=[],state=[],nodes=[],derived=[],controls=[],actions=[],views=[],root='root')
    def state(self,name,type,value):self.p['state'].append(dict(name=name,type=type,initial=json.dumps(value)));return '$state.'+name
    def record(self,record_name,**fields):self.p['records'].append(dict(name=record_name,fields=[dict(name=k,type=v) for k,v in fields.items()]))
    def node(self,node_id,op,*args,**params):
        if op=='literal':params['value']=json.dumps(params['value'])
        if 'fields' in params:params['fields']=[dict(field=k,node=v) for k,v in params['fields'].items()]
        self.p['nodes'].append(dict(id=node_id,op=op,args=list(args),params=params));return node_id
    def lit(self,id,value):return self.node(id,'literal',type='boolean' if isinstance(value,bool) else 'number' if isinstance(value,(int,float)) else 'string',value=value)
    def derived(self,field,node):self.p['derived'].append(dict(field=field,node=node))
    def control(self,field,label,widget='integer',minimum=0,maximum=100000,step=1,invalidate=()):self.p['controls'].append(dict(field=field,label=label,widget=widget,minimum=minimum,maximum=maximum,step=step,invalidate=list(invalidate)))
    def action(self,action_id,updates=(),steps=(),**fields):self.p['actions'].append(dict(id=action_id,fields=[dict(name=k,type=v) for k,v in fields.items()],updates=[dict(field=f,node=n) for f,n in updates],steps=list(steps)))
    def view(self,id,component,children=(),events=(),each='',key='',when='',**props):self.p['views'].append(dict(id=id,component=component,props=[dict(name=k,node=v) for k,v in props.items()],events=list(events),children=list(children),each=each,key=key,when=when));return id
    def event(self,event_name,action,**fields):return dict(name=event_name,action=action,payload=[dict(field=k,node=v) for k,v in fields.items()])
    def call(self,id,module,function,args=(),bind='',node='',when=''):return dict(kind='call',id=id,module=module,function=function,args=list(args),bind=bind,node=node,when=when)
    def set(self,bind,node):return dict(kind='set',bind=bind,node=node)
    def save(self,out,name): (out/(name+'.json')).write_text(json.dumps(self.p,indent=2))

def budget():
 p=Plan();a=p.state('approved','number',18000);b=p.state('committed','number',19750);d=p.state('difference','number',0);m=p.state('magnitude','number',0);s=p.state('status','string','')
 p.derived('difference',p.node('subtract','subtract',b,a));p.derived('magnitude',p.node('abs','abs',d));zero=p.lit('zero',0)
 over=p.node('over','gt',d,zero);under=p.node('under','lt',d,zero)
 p.derived('status',p.node('status','choose',over,p.lit('overText','Over budget'),p.node('other','choose',under,p.lit('underText','Under budget'),p.lit('equalText','Equal'))))
 p.control('approved','Approved','money',step=100);p.control('committed','Committed','money',step=100)
 p.view('money','Money',label=p.lit('differenceLabel','Difference'),minorUnits=m)
 p.view('root','Comparison',children=['money'],status=s);return p

def departure():
 p=Plan();start=p.state('start','number',960);distance=p.state('distance','number',12);unload=p.state('unload','number',15);inspect=p.state('inspect','number',10);travel=p.state('travel','number',0);loaded=p.state('loaded','boolean',False);leave=p.state('leave','number',0);check=p.state('inspection','number',0);lt=p.state('leaveText','string','');it=p.state('inspectionText','string','')
 p.derived('leave',p.node('leave','subtract',p.node('arrival','subtract',start,unload),travel));p.derived('inspection',p.node('inspection','subtract',leave,inspect));p.derived('leaveText',p.node('clockLeave','clock',leave));p.derived('inspectionText',p.node('clockInspect','clock',check))
 p.control('start','Start','time',maximum=1439);p.control('distance','Distance (km)',minimum=1,maximum=1000,invalidate=['loaded']);p.control('unload','Unloading minutes',maximum=180);p.control('inspect','Inspection minutes',maximum=180)
 p.action('Load',steps=[p.call('estimate','host/trip','estimate',[distance],bind='travel',node='$estimate'),p.set('loaded',p.lit('true',True))])
 p.view('load','PrimaryAction',text=p.lit('loadText','Get travel estimate'),events=[p.event('onClick','Load')]);p.view('leaveView','Metric',when=loaded,label=p.lit('departLabel','Depart by'),value=lt);p.view('inspectView','Metric',when=loaded,label=p.lit('inspectLabel','Inspection'),value=it)
 p.view('root','Column',children=['load','leaveView','inspectView']);return p

def food():
 p=Plan();p.record('Pizza',id='string',name='string',cents='number',vegan='boolean',diameterMm='number',quantity='number');p.record('Window',id='string',start='string',end='string',eligible='boolean',cents='number')
 rows=p.state('rows','Pizza[]',[]);windows=p.state('windows','Window[]',[]);selected=p.state('selected','Window',dict(id='',start='',end='',eligible=False,cents=0));required=p.state('required','number',6);vegan=p.state('vegan','number',2);budget=p.state('budget','number',8000);total=p.state('total','number',0);count=p.state('count','number',0);diet=p.state('diet','number',0);valid=p.state('valid','boolean',False);status=p.state('status','string','Load menu and delivery');loaded=p.state('loaded','boolean',False)
 p.lit('zero',0);p.lit('true',True);p.lit('false',False)
 p.node('menuRow','record',type='Pizza',fields=dict(id='$row.id',name='$row.name',cents='$row.cents',vegan='$row.vegan',diameterMm='$row.diameterMm',quantity='zero'))
 p.node('menuProject','project','$menu','menuRow');p.node('sizeEligible','eq','$row.diameterMm',p.lit('diameter',300));p.node('menuFilter','filter','menuProject','sizeEligible')
 p.node('windowRow','record',type='Window',fields=dict(id='$row.id',start='$row.from',end='$row.until',eligible='$row.meetsRequirement',cents='$row.deliveryCents'));p.node('windowProject','project','$delivery','windowRow');p.node('eligibleWindows','filter',windows,'$row.eligible');p.node('firstWindow','first','eligibleWindows')
 p.node('allocationBudget','subtract',budget,'$state.selected.cents');p.node('allocation','allocate',rows,required,'allocationBudget',quantity='quantity',cost='cents',quotas=[dict(field='vegan',minimum=vegan)])
 p.action('Load',steps=[p.call('menu','host/pizza','menu',bind='rows',node='menuFilter'),p.call('delivery','host/pizza','delivery',[p.lit('requiredBy','18:30')],bind='windows',node='windowProject'),p.set('selected','firstWindow'),p.set('rows','allocation'),p.set('loaded','true')])
 p.node('lineCost','multiply','$row.quantity','$row.cents');p.node('foodTotal','sum',rows,'lineCost');p.derived('total',p.node('total','add','foodTotal','$state.selected.cents'));p.derived('count',p.node('count','sum',rows,'$row.quantity'));p.node('veganQuantity','choose','$row.vegan','$row.quantity','zero');p.derived('diet',p.node('diet','sum',rows,'veganQuantity'))
 p.node('countOk','eq',count,required);p.node('dietOk','ge',diet,vegan);p.node('costOk','le',total,budget);p.node('someValid','and','countOk','dietOk');p.node('validCost','and','someValid','costOk');p.node('validWindow','and','validCost','$state.selected.eligible');p.derived('valid',p.node('valid','and',loaded,'validWindow'));p.derived('status',p.node('status','choose',valid,p.lit('ready','Ready'),p.lit('invalid','Not feasible')))
 p.node('keyMatch','eq','$row.id','$action.id');p.node('changedRows','update',rows,'keyMatch',fields=dict(quantity='$action.value'));p.node('wholePayload','eq','$action.value',p.node('payloadFloor','floor','$action.value'));p.node('positivePayload','ge','$action.value','zero');p.node('boundedPayload','le','$action.value',p.lit('quantityMaximum',32));p.node('rangePayload','and','positivePayload','boundedPayload');p.node('validPayload','and','wholePayload','rangePayload');p.node('safeRows','choose','validPayload','changedRows',rows);p.action('Quantity',updates=[('rows','safeRows')],id='string',value='number')
 p.node('chosenWindow','lookup',windows,'$action.id',key='id');p.action('SelectWindow',updates=[('selected','chosenWindow')],id='string')
 p.action('Allocate',updates=[('rows','allocation')])
 p.control('budget','Budget','money',step=100);p.control('required','Pizzas',maximum=32);p.control('vegan','Minimum vegan',maximum=32)
 p.view('load','PrimaryAction',text=p.lit('loadText','Load menu and delivery'),events=[p.event('onClick','Load')])
 p.view('rowQty','IntField',label='$row.name',value='$row.quantity',minimum='zero',maximum=p.lit('maxQty',32),events=[p.event('onChange','Quantity',id='$row.id',value='$payload')])
 p.view('rows','Item',each=rows,key='$row.id',title='$row.name',children=['rowQty'])
 p.view('windows','Option',each=windows,key='$row.id',value='$row.id',title='$row.end',events=[p.event('onSelect','SelectWindow',id='$payload')])
 p.view('money','Money',label=p.lit('totalLabel','Total'),minorUnits=total);p.view('countView','Metric',label=p.lit('pizzasLabel','Pizzas'),value=p.state('countText','string',''));p.derived('countText',p.node('countText','integer',count))
 p.view('comparison','Comparison',status=status,children=['money','countView']);p.view('allocateButton','SecondaryAction',text=p.lit('allocateLabel','Recalculate allocation'),events=[p.event('onClick','Allocate')]);p.view('root','Column',children=['load','allocateButton','rows','windows','comparison']);return p

def equipment():
 p=Plan();p.record('Component',itemId='string',quantity='number');p.record('Attributes',kind='string',adjustable='boolean',clipAdapter='boolean',balanced='boolean',connectorFrom='string',connectorTo='string',lengthMm='number');p.record('Equipment',id='string',name='string',cents='number',available='number',attributes='Attributes',components='Component[]',quantity='number');p.record('Quote',cents='number',depositCents='number',available='boolean',ttl='number')
 rows=p.state('rows','Equipment[]',[]);packages=p.state('packages','Equipment[]',[]);separate=p.state('separate','Equipment[]',[]);package=p.state('package','Equipment[]',[]);budget=p.state('budget','number',2000);from_=p.state('from','string','2026-10-23T14:15:00Z');until=p.state('until','string','2026-10-23T20:30:00Z');loaded=p.state('loaded','boolean',False)
 emptyquote=dict(cents=0,depositCents=0,available=False,ttl=0)
 p.state('packageQuote','Quote',emptyquote);p.state('separateQuote','Quote',emptyquote);status=p.state('status','string','');p.state('packageUsable','boolean',False);p.state('separateUsable','boolean',False);now=p.state('now','number',0);p.state('fresh','boolean',False);p.state('expiresAt','number',0)
 p.lit('zero',0);p.lit('one',1);p.lit('true',True)
 p.node('attributeRow','record',type='Attributes',fields={k:'$row.attributes.'+k for k in ['kind','adjustable','clipAdapter','balanced','connectorFrom','connectorTo','lengthMm']});p.node('componentRow','record',type='Component',fields=dict(itemId='$row.itemId',quantity='$row.quantity'));p.node('components','project','$row.components','componentRow')
 p.node('catalogueRow','record',type='Equipment',fields=dict(id='$row.id',name='$row.name',cents='$row.cents',available='$row.available',attributes='attributeRow',components='components',quantity='one'));p.node('catalogue','project','$catalogue','catalogueRow')
 p.node('standKind','eq','$row.attributes.kind',p.lit('stand','stand'));p.node('standA','and','standKind','$row.attributes.adjustable');p.node('standOk','and','standA','$row.attributes.clipAdapter')
 p.node('cableKind','eq','$row.attributes.kind',p.lit('cable','cable'));p.node('cableA','and','cableKind','$row.attributes.balanced');p.node('female','eq','$row.attributes.connectorFrom',p.lit('fromConnector','XLR female'));p.node('male','eq','$row.attributes.connectorTo',p.lit('toConnector','XLR male'));p.node('ends','and','female','male');p.node('longEnough','ge','$row.attributes.lengthMm',p.lit('length',10000));p.node('cableB','and','cableA','ends');p.node('cableOk','and','cableB','longEnough')
 p.node('standRows','filter',rows,'standOk');p.node('cableRows','filter',rows,'cableOk');p.node('firstStand','first','standRows');p.node('firstCable','first','cableRows');p.node('separateOne','array','firstStand','firstCable',element='Equipment');p.node('hasStand','gt',p.node('standCount','length','standRows'),'zero');p.node('hasCable','gt',p.node('cableCount','length','cableRows'),'zero');p.node('hasSeparate','and','hasStand','hasCable');p.node('separateRows','choose','hasSeparate','separateOne','emptyBasket')
 p.node('packageKind','eq','$row.attributes.kind',p.lit('packageKindText','package'));p.node('packageRows','filter',rows,'packageKind');p.node('matchingPackages','covers','packageRows',rows,components='components',key='id',itemId='itemId',quantity='quantity',groups=[dict(predicate='standOk',minimum='one'),dict(predicate='cableOk',minimum='one')]);p.node('firstPackage','first','matchingPackages');p.node('packageOne','array','firstPackage',element='Equipment');p.node('emptyBasket','array',element='Equipment');p.node('hasMatchingPackage','gt',p.node('packageCount','length','matchingPackages'),'zero');p.node('packageBasket','choose','hasMatchingPackage','packageOne','emptyBasket')
 p.node('packageItems','basket',package,id='id',quantity='quantity');p.node('separateItems','basket',separate,id='id',quantity='quantity')
 for id in ['packageResult','separateResult']:p.node(id+'Record','record',type='Quote',fields=dict(cents='$'+id+'.cents',depositCents='$'+id+'.depositCents',available='$'+id+'.available',ttl='$'+id+'.expiresInMinutes'))
 p.action('Load',updates=[('packageQuote',p.node('emptyQuote','literal',type='Quote',value=emptyquote)),('separateQuote','emptyQuote'),('fresh',p.lit('notFresh',False))],steps=[p.call('catalogue','host/rental','equipment',[from_,until],bind='rows',node='catalogue'),p.set('packages','matchingPackages'),p.set('package','packageBasket'),p.set('separate','separateRows'),p.call('packageResult','host/rental','quote',['packageItems',from_,until],bind='packageQuote',node='packageResultRecord',when=p.node('hasPackage','ne','packageItems',p.lit('emptyId',''))),p.call('separateResult','host/rental','quote',['separateItems',from_,until],bind='separateQuote',node='separateResultRecord',when=p.node('hasSeparateItems','ne','separateItems','emptyId')),p.set('loaded','true'),p.set('fresh','true'),p.set('expiresAt',p.lit('awaitingReceipt',-1))])
 p.node('isResume','lt','$action.epoch','zero');p.node('absoluteEpoch','abs','$action.epoch');p.node('awaiting','lt','$state.expiresAt','zero');p.node('packageFirst','le','$state.packageQuote.ttl','$state.separateQuote.ttl');p.node('minimumTtl','choose','packageFirst','$state.packageQuote.ttl','$state.separateQuote.ttl');p.node('ttlMillis','multiply','minimumTtl',p.lit('minuteMillis',60000));p.node('receiptExpiry','add','$state.now','ttlMillis');p.node('expiry','choose','awaiting','receiptExpiry','$state.expiresAt');p.node('beforeExpiry','lt','absoluteEpoch','expiry');p.node('notBackwards','ge','absoluteEpoch','$state.now');p.node('trustedClock','and','notBackwards','beforeExpiry');p.node('stillFresh','and','$state.fresh','trustedClock');p.action('Tick',updates=[('now','absoluteEpoch'),('expiresAt','expiry'),('fresh',p.node('fresh','choose','isResume',p.lit('false',False),'stillFresh'))],epoch='number')
 for prefix in ['package','separate']:
  p.node(prefix+'Within','le','$state.'+prefix+'Quote.cents',budget);p.node(prefix+'Available','and',prefix+'Within','$state.'+prefix+'Quote.available');p.node(prefix+'Loaded','and',loaded,prefix+'Available');p.derived(prefix+'Usable',p.node(prefix+'Usable','and','$state.fresh',prefix+'Loaded'))
 p.node('anyUsable','or','$state.packageUsable','$state.separateUsable');p.derived('status',p.node('status','choose','anyUsable',p.lit('ready','Within budget'),p.lit('invalid','No usable quote')))
 p.control('budget','Hire budget','money',step=100)
 p.view('load','PrimaryAction',text=p.lit('loadText','Read equipment and quote alternatives'),events=[p.event('onClick','Load')]);p.view('packageHire','Money',label=p.lit('packageLabel','Package hire'),minorUnits='$state.packageQuote.cents');p.view('separateHire','Money',label=p.lit('separateLabel','Separate hire'),minorUnits='$state.separateQuote.cents');p.view('packageDeposit','Money',label=p.lit('depositLabel','Refundable package deposit'),minorUnits='$state.packageQuote.depositCents');p.view('comparison','Comparison',status=status,children=['packageHire','separateHire','packageDeposit']);p.view('clock','SessionClock',events=[p.event('onTick','Tick',epoch='$payload')]);p.view('root','Column',children=['clock','load','comparison']);return p

def fragments():
 def p(title,b):return dict(title=title,behaviors=[b])
 cap=lambda module,function:dict(module=module,function=function)
 return {
 'fragment-budget':p('Budget reconciliation',dict(kind='comparison',limit=18000,actual=19750,limitLabel='Approved',actualLabel='Committed')),
 'fragment-departure':p('Departure',dict(kind='providerSchedule',start=960,input=12,before=15,inspection=10,estimate=cap('host/trip','estimate'))),
 'fragment-food':p('Crew dinner',dict(kind='allocation',total=6,minimum=2,budget=8000,size=300,deadline=1110,menu=cap('host/pizza','menu'),delivery=cap('host/pizza','delivery'),fields=dict(id='id',name='name',cost='cents',category='vegan',size='diameterMm',windowId='id',windowStart='from',windowEnd='until',eligible='meetsRequirement',deliveryCost='deliveryCents'))),
 'fragment-equipment':p('Equipment hire',dict(kind='quotedAlternatives',budget=2000,**{'from':'2026-10-23T14:15:00Z','until':'2026-10-23T20:30:00Z'},catalogue=cap('host/rental','equipment'),quote=cap('host/rental','quote'),fields=dict(id='id',name='name',cost='cents',stock='available',components='components',componentId='itemId',componentQuantity='quantity',quoteCost='cents',deposit='depositCents',available='available',ttl='expiresInMinutes'),groups=[dict(label='Stand',constraints=[dict(field='kind',operator='eq',value='stand'),dict(field='adjustable',operator='eq',value=True),dict(field='clipAdapter',operator='eq',value=True)]),dict(label='Cable',constraints=[dict(field='kind',operator='eq',value='cable'),dict(field='balanced',operator='eq',value=True),dict(field='connectorFrom',operator='eq',value='XLR female'),dict(field='connectorTo',operator='eq',value='XLR male'),dict(field='lengthMm',operator='ge',value=10000)])]))}

def custom_budget():
 p=budget();p.p['custom']=[dict(id='difference',parameters=[dict(name='left',type='number'),dict(name='right',type='number')],result='number',body='return right - left;')];p.p['reason']='Explicit custom-section test only; maintained subtract exists and production must not use this reason.'
 n=next(n for n in p.p['nodes'] if n['id']=='subtract');n.update(op='custom',args=['$state.approved','$state.committed'],params=dict(section='difference'));return p

def join_result():
 p=Plan();p.record('Row',id='string',value='number');p.record('Joined',id='string',total='number');left=p.state('left','Row[]',[dict(id='opaque-b',value=2),dict(id='opaque-a',value=3)]);right=p.state('right','Row[]',[dict(id='opaque-a',value=7),dict(id='opaque-b',value=9)]);p.state('joined','Joined[]',[])
 p.node('same','eq','$left.id','$right.id');p.node('total','add','$left.value','$right.value');p.node('projection','record',type='Joined',fields=dict(id='$left.id',total='total'));p.derived('joined',p.node('joined','join',left,right,'same','projection'));p.view('root','IntText',value=p.lit('label',0));return p

def optional_result():
 p=Plan();present=p.state('present','boolean',False);value=p.state('value','number',0);p.action('Load',steps=[p.call('result','host/optional','read',bind='present',node='$result.amount.present'),p.set('value',p.node('value','choose','$result.amount.present','$result.amount.value',p.lit('missing',0)))])
 p.state('display','string','');p.derived('display',p.lit('format','Optional value'));p.view('load','PrimaryAction',text=p.lit('loadText','Read optional'),events=[p.event('onClick','Load')]);p.view('value','Metric',label=p.lit('valueLabel','Value'),value='$state.display');p.view('root','Column',children=['load','value']);return p

def contracts():
 def ty(kind,**kw):return dict(kind=kind,maximum=kw.pop('maximum',128),minimumNumber=-1.7976931348623157e308,maximumNumber=1.7976931348623157e308,**kw)
 def rec(**fields):return ty('record',fields=fields)
 def arr(element):return ty('array',element=element)
 def fn(name,result,**params):return dict(name=name,description='Test fixture boundary',parameters=[dict(name=k,type=v) for k,v in params.items()],result=result)
 n=ty('number');s=ty('string');b=ty('boolean')
 rental=arr(rec(id=s,name=s,cents=n,available=n,attributes=rec(kind=s,adjustable=b,clipAdapter=b,balanced=b,connectorFrom=s,connectorTo=s,lengthMm=n),components=arr(rec(itemId=s,quantity=n))))
 optional=rec(amount=n);optional['optionalFields']=['amount']
 return [dict(version=1,module='host/optional',description='Test',functions=[fn('read',optional)]),dict(version=1,module='host/rental',description='Test',functions=[fn('equipment',rental,from_=s,until=s),fn('quote',rec(cents=n,depositCents=n,available=b,expiresInMinutes=n),items=s,from_=s,until=s)]),dict(version=1,module='host/trip',description='Test',functions=[fn('estimate',n,distanceKm=n)]),dict(version=1,module='host/pizza',description='Test',functions=[fn('menu',arr(rec(id=s,name=s,cents=n,vegan=b,diameterMm=n))),fn('delivery',arr(rec(id=s,**{'from':s,'until':s},meetsRequirement=b,deliveryCents=n)),requiredBy=s)])]

if __name__=='__main__':
 import sys
 out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
 for name,f in [('budget',budget),('departure',departure),('food',food),('equipment',equipment),('custom-budget',custom_budget),('optional',optional_result),('join',join_result)]:f().save(out,name)
 for name,p in fragments().items():
  for behavior in p['behaviors']:
   for group in behavior.get('groups',[]):
    for constraint in group['constraints']:
     value=constraint.pop('value');constraint.update(valueType='boolean' if isinstance(value,bool) else 'number' if isinstance(value,(int,float)) else 'string',text=value if isinstance(value,str) else '',number=value if isinstance(value,(int,float)) and not isinstance(value,bool) else 0,boolean=value if isinstance(value,bool) else False)
  (out/(name+'.json')).write_text(json.dumps(dict(title=p['title'],**p['behaviors'][0]),indent=2))
 (out/'contracts.json').write_text(json.dumps(contracts(),indent=2))
