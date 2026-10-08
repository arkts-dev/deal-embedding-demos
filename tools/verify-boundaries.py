#!/usr/bin/env python3
"""Source ownership and real artifact consumption, no intended-role assumptions."""
from pathlib import Path
import io, os, zipfile
root = Path(__file__).resolve().parents[1]
for obsolete in ['android/embedding', 'android/connectors', 'android/shell', 'experiences',
                 'android/apps/calendar/assets', 'android/apps/todo/api', 'android/apps/vehicle/api',
                 'android/apps/calendar/src/ConnectorBroker.kt', 'android/apps/calendar/src/ProviderAdapters.kt']:
    assert not (root / obsolete).exists(), obsolete
library = root / 'dependencies/deal-embedding'
expected = {'ExperienceRuntime.kt'}
assert {p.name for p in (library/'android/src').glob('*.kt')} == expected
assert {p.name for p in (library/'android/src/capabilities').glob('*.kt')} == {
    'CapabilityContract.kt','CapabilityRegistry.kt','CapabilityDiscovery.kt','CapabilityService.kt'}
assert (library/'core/generation.deal').exists()
assert not (library/'backends').exists()
assert {p.name for p in (library/'core/host').glob('*.d.deal')} == {'checker.d.deal', 'discovery.d.deal', 'model.d.deal', 'chooser.d.deal', 'policy-data.d.deal', 'observer.d.deal'}
assert (library/'android/src/compiler/ExperienceCompiler.kt').exists()
assert {p.name for p in (library/'android/src/bindings').glob('*.kt')} == {'CapabilityBindings.kt', 'DealSessionBindings.kt', 'ExperienceGenerator.kt', 'SandboxProgram.kt', 'StageReceipts.kt'}
assert {p.name for p in (library/'android/src/inference').glob('*.kt')} == {'ModelClient.kt'}
assert (root/'android/apps/development/GenerationGateway.kt').exists()
assert 'getSharedPreferences' not in (library/'android/src/capabilities/CapabilityService.kt').read_text()
assert 'configureDealCapabilities' not in (library/'android/assets/embedding/bindings/generation-session.js').read_text()
assert (library/'android/src/storage/ExperienceStorage.kt').exists()
for path in (library / 'android').rglob('*'):
    if path.is_file():
        text = path.read_text()
        assert 'dev.deal.apps' not in text and 'host/todo' not in text and 'host/trip' not in text and 'host/calendar' not in text, path
for path in (root/'android/apps/calendar/src').glob('*.kt'):
    text=path.read_text()
    assert 'dev.deal.apps.todo' not in text and 'dev.deal.apps.vehicle' not in text and 'host/todo' not in text and 'host/trip' not in text, path
    assert 'distanceKm' not in text and 'makeTable' not in text, path
for provider, other in [('todo', 'vehicle'), ('vehicle', 'todo')]:
    for path in (root / f'android/apps/{provider}').rglob('*'):
        if path.is_file(): assert f'dev.deal.apps.{other}' not in path.read_text(), path
with zipfile.ZipFile(os.environ.get('EMBEDDING_AAR', root / 'build/embedding-library/deal-embedding.aar')) as aar:
    with zipfile.ZipFile(io.BytesIO(aar.read('classes.jar'))) as classes:
        names = classes.namelist()
        assert 'dev/deal/embedding/capabilities/ICapabilityService.class' in names
        assert 'deal/Main.class' in names and 'deal/ui/UiSourceGenerator.class' in names
        assert not any(name.startswith('dev/deal/apps/') for name in names)
        assert 'dev/deal/embedding/GenerationGateway.class' not in names
        assert not any('ChoiceCatalogue' in name for name in names)
    with zipfile.ZipFile(root / os.environ.get('HOST_APK', 'build/organizer.apk')) as app:
        for name in aar.namelist():
            if name.startswith('assets/') and not name.endswith('/'): assert app.read(name) == aar.read(name), name
        assert not any('connectors.js' in name for name in app.namelist())
        assert 'assets/generation/generation.js' in app.namelist()
        assert b'for (' in app.read('assets/generation/generation.js')
        assert 'assets/embedding/bindings/generation-session.js' in app.namelist()
        assert app.read('assets/embedding/bindings/dealui-session.js') == (root/'dependencies/deal-ui/runtime-js/session.js').read_bytes()
        contracts = app.read('assets/embedding/bindings/generation-contracts.js').decode()
        import json
        metadata = json.loads(contracts.removeprefix('configureDealCapabilities(').strip().removesuffix(');'))
        assert {m['module'] for m in metadata} == {'embedding/checker', 'embedding/model', 'embedding/discovery', 'embedding/chooser', 'embedding/policy-data', 'embedding/observer'}
        checker = next(m for m in metadata if m['module'] == 'embedding/checker')
        assert checker['functions'][0]['result'] == json.loads((library/'core/host/check-result.json').read_text())
        assert [p['name'] for p in checker['functions'][0]['parameters']] == ['deal', 'dealui']
        chooser = next(m for m in metadata if m['module'] == 'embedding/chooser')
        assert [f['name'] for f in chooser['functions']] == ['choose']
        for module in ['catalogue-source', 'candidate-check', 'generation-guidance', 'source-literals']:
            assert f'assets/generation/{module}.js' in app.namelist()
        assert not any(n.startswith('assets/deal-backend/') for n in app.namelist())
for provider, other in [('todo', 'vehicle'), ('vehicle', 'todo')]:
    with zipfile.ZipFile(root / f'build/{provider}.apk') as apk:
        dex=b''.join(apk.read(name) for name in apk.namelist() if name.endswith('.dex'))
        assert b'Ldev/deal/embedding/capabilities/ICapabilityService;' in dex
        assert f'Ldev/deal/apps/{other}/'.encode() not in dex
        assert b'Ldeal/Main;' not in dex and b'Ldev/deal/embedding/ExperienceRuntime;' not in dex
# Compare credential bytes without emitting them or their configuration.
import json
config=json.loads((Path.home()/'.pi/agent/models.json').read_text())['providers']['devagent']
key=config.get('apiKey','')
if len(key)>16 and not key.startswith('!'):
    for path in [root/os.environ.get('HOST_APK', 'build/organizer.apk'), Path(os.environ.get('EMBEDDING_AAR', root/'build/embedding-library/deal-embedding.aar'))]:
        with zipfile.ZipFile(path) as archive:
            assert all(key.encode() not in archive.read(n) for n in archive.namelist() if not n.endswith('/')), 'Credential found in artifact'
print('Core host contracts and Android compiler/bindings separated; compiled DEAL orchestration packaged; no external wiring or credentials; AAR assets consumed byte-for-byte')
