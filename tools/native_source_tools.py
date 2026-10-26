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
INITIAL=obj({'name':TEXT,'type':TEXT,'value':TEXT})
COMPACT_PROPERTIES={
 'imports':arr(obj({'alias':TEXT,'module':TEXT})),
 'records':arr(obj({'name':TEXT,'fields':arr(FIELD)})),
 'state':arr(INITIAL),
 'derived':arr(obj({'name':TEXT,'type':TEXT,'value':TEXT,'depends':arr(TEXT),'expression':TEXT})),
 'controls':arr(obj({'field':TEXT,'label':TEXT,'format':{'type':'string','enum':['integer','euro-cents']},'minimum':{'type':'number'},'maximum':{'type':'number'},'step':{'type':'number'},'invalidate':arr(TEXT)})),
 'actions':arr(obj({'name':TEXT,'fields':arr(FIELD),'body':TEXT})),
 'effects':arr(obj({'name':TEXT,'label':TEXT,'body':TEXT})),
 'functions':TEXT,'view':TEXT
}
COMPACT_TOOLS=[tool('submit_compact','Materialize one typed compact candidate into canonical DEAL and Deal UI; validate and compile it atomically. No provider execution.',COMPACT_PROPERTIES,list(COMPACT_PROPERTIES)),
 tool('source_fallback','Explicitly select full-source generation only when the compact contract cannot express the task. All elapsed work remains charged.',{'reason':TEXT},['reason'])]

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
    return stable_messages(dict(body,input=prefix))+transcript

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
