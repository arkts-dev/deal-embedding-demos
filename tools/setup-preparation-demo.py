#!/usr/bin/env python3
"""Create labelled Calendar/Todo fixtures through debug launch extras. Never enable grants."""
import argparse, subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('scenario',choices=['viewing','budget','ambiguous','unrelated','held-out','friday'])
p.add_argument('--date',help='Exact fixture date (YYYY-MM-DD); defaults to tomorrow, except friday uses next Friday')
a=p.parse_args()
task,event,note={
 'viewing':('Download last three payslips','Apartment viewing','Bring three payslips, printed.'),
 'budget':('Check revised vendor quotation','Project budget review','Compare the revised quotation with the approved budget.'),
 'ambiguous':('Download the report','Review meeting','Bring the documents, if relevant.'),
 'unrelated':('Check revised vendor quotation','Swimming lesson','Bring a towel.'),
 'held-out':('Fetch company incorporation certificate','Client onboarding','Verify business registration.'),
 'friday':('Export the revised input-to-channel list','Soundcheck preparation','Bring the current channel assignments.')
}[a.scenario]
def launch(app, extras):
 package='dev.deal.apps.'+app
 subprocess.run(['adb','shell','am','force-stop',package],check=True)
 # Quote each value for the device shell, not merely the local argv.
 import shlex
 args=['am','start','-W','-n',package+('/.AgendaActivity' if app=='calendar' else '/.TodoActivity')]
 for key,value in extras.items():args+=['--es',key,value]
 subprocess.run(['adb','shell',' '.join(map(shlex.quote,args))],check=True)
launch('todo',{'demoTask':task})
extras={'demoEvent':event,'demoNote':note}
if a.date:extras['demoDate']=a.date
elif a.scenario=='friday':
 from datetime import date,timedelta
 # Device calendar uses Europe/Berlin, not the workstation's clock.
 stamp=subprocess.check_output(['adb','shell','TZ=Europe/Berlin date +%Y-%m-%d'],text=True).strip()
 today=date.fromisoformat(stamp);extras['demoDate']=(today+timedelta(days=(4-today.weekday())%7 or 7)).isoformat()
launch('calendar',extras)
print('Labelled fixtures prepared; permissions unchanged. Open Calendar normally after configuring Extensions.')
