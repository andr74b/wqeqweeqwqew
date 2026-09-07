import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const desktopDirectory = path.resolve(scriptDirectory, '..');
const projectDirectory = path.resolve(desktopDirectory, '..');
const argumentsAfterSeparator = process.argv.slice(2);
const option = (name) => {
  const index = argumentsAfterSeparator.indexOf(name);
  return index >= 0 ? argumentsAfterSeparator[index + 1] : undefined;
};

const requestedTarget = option('--target');
const target = requestedTarget || (process.platform === 'darwin' ? 'mac'
  : process.platform === 'win32' ? 'win' : undefined);
if (!target) throw new Error('Desktop installers can only be built on macOS or Windows.');
if ((target === 'mac') !== (process.platform === 'darwin')) {
  throw new Error('Build macOS installers on macOS and Windows installers on Windows so the bundled JVM matches.');
}

function run(command, args, cwd) {
  const result = spawnSync(command, args, { cwd, stdio: 'inherit', shell: false });
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status ?? 1);
}

const maven = path.join(projectDirectory, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
run(maven, ['--batch-mode', '--no-transfer-progress', 'clean', 'verify'], projectDirectory);
run(process.execPath, [path.join(scriptDirectory, 'prepare-runtime.mjs')], desktopDirectory);

const builder = path.join(desktopDirectory, 'node_modules', '.bin',
  process.platform === 'win32' ? 'electron-builder.cmd' : 'electron-builder');
if (!fs.existsSync(builder)) {
  throw new Error('Desktop dependencies are missing. Run npm ci in the desktop directory first.');
}
const builderArguments = target === 'mac' ? ['--mac', 'dmg', 'zip'] : ['--win', 'nsis', 'msi'];
const architecture = option('--arch');
if (architecture) {
  if (!['x64', 'arm64'].includes(architecture)) throw new Error(`Unsupported architecture: ${architecture}`);
  builderArguments.push(`--${architecture}`);
}
builderArguments.push('--publish', 'never');
run(builder, builderArguments, desktopDirectory);
