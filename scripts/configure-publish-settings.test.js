import assert from 'node:assert/strict'
import { execFileSync, spawnSync } from 'node:child_process'
import {
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync
} from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { afterEach, beforeEach, describe, test } from 'node:test'

const script = fileURLToPath(
  new URL('./configure-publish-settings.js', import.meta.url)
)
const RUN_ID = '1658821493'
const RUN_ATTEMPT = '1'
const RUN_SUFFIX = `-run-${RUN_ID}-attempt-${RUN_ATTEMPT}`
let directory
let outputFile

beforeEach(() => {
  directory = mkdtempSync(join(tmpdir(), 'configure-publish-settings-'))
  outputFile = join(directory, 'output')
  writeFileSync(outputFile, '')
  git('init', '--quiet')
  git('commit', '--allow-empty', '--quiet', '-m', 'Initial commit')
})

afterEach(() => {
  rmSync(directory, { recursive: true, force: true })
})

function git(...args) {
  return execFileSync(
    'git',
    [
      '-c',
      'user.name=Publishing test',
      '-c',
      'user.email=publishing-test@example.invalid',
      '-c',
      'commit.gpgsign=false',
      '-c',
      'tag.gpgsign=false',
      '-c',
      'core.hooksPath=/dev/null',
      ...args
    ],
    {
      cwd: directory,
      env: {
        ...process.env,
        GIT_CONFIG_GLOBAL: '/dev/null',
        GIT_CONFIG_NOSYSTEM: '1'
      },
      encoding: 'utf8'
    }
  )
}

function runScript(branch, { runId = RUN_ID, runAttempt = RUN_ATTEMPT } = {}) {
  const env = { ...process.env }
  delete env.GITHUB_REF_NAME
  delete env.GITHUB_OUTPUT
  delete env.GITHUB_RUN_ID
  delete env.GITHUB_RUN_ATTEMPT
  env.GITHUB_REF_NAME = branch
  env.GITHUB_OUTPUT = outputFile
  if (runId) {
    env.GITHUB_RUN_ID = runId
  }
  if (runAttempt) {
    env.GITHUB_RUN_ATTEMPT = runAttempt
  }
  return spawnSync(process.execPath, [script], {
    env,
    cwd: directory,
    encoding: 'utf8'
  })
}

describe('configure publish settings', () => {
  test('preserves stable releases and Docker Hub publishing on main', () => {
    git('tag', '0.4.0')
    const result = runScript('main')

    assert.equal(result.status, 0)
    assert.equal(result.stderr, '')
    assert.equal(
      readFileSync(outputFile, 'utf8'),
      'prerelease=false\nprerelease-suffix=\npush-dockerhub=true\n'
    )
  })

  test("creates a unique ECR-only prerelease for Marina's feature branch", () => {
    const result = runScript('feature/FISB-72-transport-details')

    assert.equal(result.status, 0)
    assert.equal(result.stderr, '')
    assert.equal(
      readFileSync(outputFile, 'utf8'),
      `prerelease=true\nprerelease-suffix=feature-fisb-72-transport-details${RUN_SUFFIX}\npush-dockerhub=false\n`
    )
  })

  test('rejects a branch head already tagged as a stable release', () => {
    git('tag', '0.4.0')

    const result = runScript('feature/FISB-72-transport-details')

    assert.equal(result.status, 1)
    assert.match(result.stderr, /already-released commit \(0\.4\.0\)/)
    assert.equal(readFileSync(outputFile, 'utf8'), '')
  })

  test('rejects #none history without an explicit version bump', () => {
    git('tag', '0.4.0')
    git('commit', '--allow-empty', '--quiet', '-m', 'Documentation update #none')

    const result = runScript('feature/FISB-72-transport-details')

    assert.equal(result.status, 1)
    assert.match(result.stderr, /contains #none without #major, #minor or #patch/)
    assert.equal(readFileSync(outputFile, 'utf8'), '')
  })

  test('rejects malformed branch names', () => {
    const result = runScript('feature/fis-notification-schemas')

    assert.equal(result.status, 1)
    assert.match(result.stderr, /Invalid publish branch/)
    assert.equal(readFileSync(outputFile, 'utf8'), '')
  })
})
