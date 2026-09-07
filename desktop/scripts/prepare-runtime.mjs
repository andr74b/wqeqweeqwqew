import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const desktopDirectory = path.resolve(scriptDirectory, '..');
const projectDirectory = path.resolve(desktopDirectory, '..');
const stagingDirectory = path.join(desktopDirectory, 'staging');
const runtimeDirectory = path.join(stagingDirectory, 'runtime');
const sourceJar = path.join(projectDirectory, 'target', 'blackjack.jar');
const stagedJar = path.join(stagingDirectory, 'blackjack.jar');

if (!process.env.JAVA_HOME) {
  throw new Error('JAVA_HOME must point to a JDK 25 installation.');
}
if (!fs.existsSync(sourceJar)) {
  throw new Error(`Build the backend first; ${sourceJar} does not exist.`);
}

const executable = (name) => path.join(process.env.JAVA_HOME, 'bin',
  process.platform === 'win32' ? `${name}.exe` : name);
const jlink = executable('jlink');
if (!fs.existsSync(jlink)) {
  throw new Error(`jlink is missing from JAVA_HOME: ${jlink}`);
}
const version = execFileSync(jlink, ['--version'], { encoding: 'utf8' }).trim();
if (Number(version.split('.')[0]) < 25) {
  throw new Error(`JDK 25 or newer is required to run this Java 25 application; found ${version}.`);
}

// jdeps reports the first group for this application. The remaining provider
// modules cover locale data, TLS, DNS, XML signatures and common Spring paths.
const modules = [
  'java.base', 'java.compiler', 'java.desktop', 'java.instrument', 'java.logging',
  'java.management', 'java.naming', 'java.net.http', 'java.prefs', 'java.rmi',
  'java.scripting', 'java.security.jgss', 'java.security.sasl', 'java.sql',
  'java.transaction.xa', 'java.xml', 'java.xml.crypto', 'jdk.charsets',
  'jdk.crypto.cryptoki', 'jdk.crypto.ec', 'jdk.jfr', 'jdk.localedata',
  'jdk.management', 'jdk.naming.dns', 'jdk.naming.rmi', 'jdk.unsupported', 'jdk.zipfs'
];

fs.rmSync(stagingDirectory, { recursive: true, force: true });
fs.mkdirSync(stagingDirectory, { recursive: true });
fs.copyFileSync(sourceJar, stagedJar);
execFileSync(jlink, [
  '--add-modules', modules.join(','),
  '--compress', 'zip-6',
  '--no-header-files',
  '--no-man-pages',
  '--strip-debug',
  '--output', runtimeDirectory
], { stdio: 'inherit' });

const java = path.join(runtimeDirectory, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
execFileSync(java, ['-version'], { stdio: 'inherit' });
console.log(`Prepared desktop backend and Java ${version} runtime in ${stagingDirectory}`);
