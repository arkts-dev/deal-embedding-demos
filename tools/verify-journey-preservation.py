#!/usr/bin/env python3
"""Verify preserved sources/state and authority from private before/after device archives.
Reads ignored build evidence; does not invoke inference, grant authority or edit the device.
"""
import base64
import hashlib
import json
import pathlib
import subprocess
import tarfile
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/enriched-journeys'


def preferences(path):
    result = {}
    with tarfile.open(path) as archive:
        for member in archive.getmembers():
            if member.name.startswith('shared_prefs/') and member.name.endswith('.xml'):
                result[member.name] = {n.get('name'): n.get('value', n.text or '')
                                       for n in ET.fromstring(archive.extractfile(member).read())}
    return result


def sources(prefs):
    pairs = {}
    for filename, values in prefs.items():
        for key, value in values.items():
            if not key.endswith('.deal'):
                continue
            prefix = key[:-5]
            if prefix + '.dealui' not in values:
                continue
            def decode(ext):
                data = values[prefix + '.' + ext]
                return base64.b64decode(data) if values.get(prefix + '.sourceEncoding') == 'base64' else data.encode()
            pairs[(filename, prefix)] = (decode('deal'), decode('dealui'))
    return pairs


receipt = {'revisions': {}, 'apps': {}}
for repo in ('.', 'dependencies/deal-embedding', 'dependencies/deal-ui', 'dependencies/deal'):
    receipt['revisions'][repo] = subprocess.check_output(['git', '-C', str(ROOT / repo), 'rev-parse', 'HEAD'], text=True).strip()
for app, after in [('organizer', 'equipment-settled.tar'), ('todo', 'food-settled.tar'), ('calendar', 'calendar-settled.tar'),
                   ('rental', 'rental-settled.tar'), ('pizza', 'pizza-settled.tar'), ('vehicle', 'vehicle-settled.tar')]:
    previous = preferences(OUT / ('before-' + app + '.tar'))
    current = preferences(OUT / after)
    before_sources, after_sources = sources(previous), sources(current)
    for key, pair in before_sources.items():
        assert after_sources.get(key) == pair, ('Accepted source changed or removed', app, key)
        filename, prefix = key
        for suffix in ('.state', '.stateSource'):
            if prefix + suffix in previous[filename]:
                assert current[filename].get(prefix + suffix) == previous[filename][prefix + suffix], ('Historical retained state changed', app, key, suffix)
    for filename, values in previous.items():
        for key, value in values.items():
            if key in ('discovery', 'generation', 'allowed', 'content') or key.startswith('grant.'):
                assert current.get(filename, {}).get(key) == value, ('Authority/disclosure/capture changed', app, filename, key)
    added = {key: pair for key, pair in after_sources.items() if key not in before_sources}
    receipt['apps'][app] = {'preservedSourcePairs': len(before_sources), 'newSourcePairs': len(added),
                            'newSourceSha256': {prefix: hashlib.sha256(d + b'\0' + u).hexdigest()
                                                for (_, prefix), (d, u) in added.items()},
                            'historicalStateAndAuthority': 'unchanged'}
    print('PASS', app, len(before_sources), 'historical exact source/state pairs preserved;', len(added), 'new pairs')
(OUT / 'preservation.json').write_text(json.dumps(receipt, indent=2) + '\n')
