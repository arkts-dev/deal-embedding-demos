"""Provider-native source operations. Only the portable core executes/admits them."""
import json
TURN_BOUNDARY='\nNATIVE TOOL TURN\n'

def tool(name, description, properties, required):
    return {'type':'function','function':{'name':name,'description':description,'strict':True,'parameters':{'type':'object','properties':properties,'required':required,'additionalProperties':False}}}
FILE={'type':'string','enum':['deal','dealui']}
SOURCE_TOOLS=[
 tool('write_file','Create one missing staged source file. Never overwrite an existing file; use edit_sources afterward.',{'file':FILE,'source':{'type':'string'}},['file','source']),
 tool('edit_sources','Atomically apply unique disjoint exact replacements to the current complete source pair. Host binds its revision. Preserve unrelated behavior.',{'edits':{'type':'array','items':{'type':'object','properties':{'file':FILE,'search':{'type':'string'},'replacement':{'type':'string'}},'required':['file','search','replacement'],'additionalProperties':False}}},['edits']),
 tool('read_file','Read one staged file, or an inclusive 1-based line range. Use null for both bounds to read the whole file. Range results contain numbered lines; their text is literal source. No other filesystem access.',{'file':FILE,'start_line':{'type':['integer','null'],'minimum':1},'end_line':{'type':['integer','null'],'minimum':1}},['file','start_line','end_line']),
 tool('check_sources','Compile the complete staged source pair. Source operations also trigger checking when both files exist.',{},[])
]

def obj(properties):
    return {'type':'object','properties':properties,'required':list(properties),'additionalProperties':False}
def arr(items): return {'type':'array','items':items}
TEXT={'type':'string'}
FIELD=obj({'name':TEXT,'type':TEXT})
# Portable behavioral contract is packaged by the embedding owner, not duplicated here.
from pathlib import Path
BEHAVIOR_SCHEMA=json.loads((Path(__file__).resolve().parents[1]/'dependencies/deal-embedding/ui/behavior-contract.json').read_text())
CUSTOM_SCHEMA=json.loads(json.dumps(BEHAVIOR_SCHEMA))
CUSTOM_SCHEMA['properties'].update({'reason':TEXT,'custom':arr(obj({'id':TEXT,'parameters':arr(FIELD),'result':TEXT,'body':TEXT}))})
CUSTOM_SCHEMA['required']=list(CUSTOM_SCHEMA['properties'])
CUSTOM_SCHEMA['properties']['nodes']['items']['anyOf'].append(obj({'id':TEXT,'op':{'type':'string','enum':['custom']},'args':arr(TEXT),'params':obj({'section':TEXT})}))
def schema_tool(name,description,schema):
    return {'type':'function','function':{'name':name,'description':description,'strict':True,'parameters':schema}}
MAINTAINED_SCHEMA=json.loads((Path(__file__).resolve().parents[1]/'dependencies/deal-embedding/ui/maintained-contract.json').read_text())
COMPACT_TOOLS=[schema_tool('submit_compact','Compose maintained state/handler/view fragments. Supply requirements and disclosed provider/field bindings, not state graphs or source. One complete behavior per screen. Emission materializes canonical DEAL/Deal UI once.',MAINTAINED_SCHEMA),
 schema_tool('submit_custom','Explicit slow path for genuinely uncovered behavior: typed maintained graph plus named custom DEAL function bodies. State why maintained operations cannot implement it. Raw bodies are measured custom coverage, never fast-path coverage.',CUSTOM_SCHEMA),
 tool('source_fallback','Last resort: explicitly select full-source generation only for behavior not representable by maintained operations or typed custom sections. All preceding work remains charged.',{'reason':TEXT},['reason'])]

def tools_for(body):
    _,sep,state=body['input'].rpartition(TURN_BOUNDARY)
    if not sep: raise ValueError('Missing native tool turn')
    state=json.loads(state)
    if state.get('compact') is True:return COMPACT_TOOLS
    if state.get('initialWrites') is True:return SOURCE_TOOLS
    return [t for t in SOURCE_TOOLS if t['function']['name']!='write_file']

def source_messages(body):
    prefix,sep,state=body['input'].rpartition(TURN_BOUNDARY)
    if not sep:raise ValueError('Missing native tool turn')
    transcript=json.loads(state)['messages']
    if not isinstance(transcript,list):raise ValueError('Invalid native tool transcript')
    from gateway_transport import stable_messages
    messages=stable_messages(dict(body,input=prefix))
    if body.get('previous'):
        messages.append({'role':'user','content':'CURRENT CANONICAL SOURCE (exact decoded pair; sole repair authority)\n'+body['previous']+'\nCURRENT CHECK DIAGNOSTICS\n'+body.get('diagnostics','')})
    return messages+transcript

def initial_write_batch(body):
    # Host-authored workspace state, not a guess from transcript text or `previous`.
    _,sep,state=body['input'].rpartition(TURN_BOUNDARY)
    if not sep:raise ValueError('Missing native tool turn')
    value=json.loads(state)
    return value.get('initialWrites') is True and value.get('compact') is not True

def native_result(message):
    # Serialize only native message fields; hidden reasoning is never forwarded or retained.
    calls=message.get('tool_calls') or []
    return json.dumps({'role':'assistant','content':message.get('content') or '', 'tool_calls':calls},ensure_ascii=False)

def strip_reasoning(data):
    for choice in data.get('choices',[]):
        for key in ('reasoning','reasoning_content'):choice.get('message',{}).pop(key,None)
