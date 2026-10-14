#!/usr/bin/env python3
"""Create labelled Calendar/Todo fixtures through debug launch extras. Never enable grants."""
import argparse, subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('scenario',choices=['viewing','budget','ambiguous','unrelated','held-out'])
a=p.parse_args()
task,event,note={
 'viewing':('Download last three payslips','Apartment viewing','Bring three payslips, printed.'),
 'budget':('Check revised vendor quotation','Project budget review','Compare the revised quotation with the approved budget.'),
 'ambiguous':('Download the report','Review meeting','Bring the documents, if relevant.'),
 'unrelated':('Check revised vendor quotation','Swimming lesson','Bring a towel.'),
 'held-out':('Fetch company incorporation certificate','Client onboarding','Verify business registration.')
}[a.scenario]
def launch(app, extras):
 package='dev.deal.apps.'+app
 subprocess.run(['adb','shell','am','force-stop',package],check=True)
 # Quote each value for the device shell, not merely the local argv.
 import shlex
 args=['am','start','-n',package+('/.AgendaActivity' if app=='calendar' else '/.TodoActivity')]
 for key,value in extras.items():args+=['--es',key,value]
 subprocess.run(['adb','shell',' '.join(map(shlex.quote,args))],check=True)
launch('todo',{'demoTask':task})
launch('calendar',{'demoEvent':event,'demoNote':note})
print('Labelled fixtures prepared; permissions unchanged. Open Calendar normally after configuring Extensions.')
