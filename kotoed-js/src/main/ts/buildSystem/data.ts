
type BuildRequest = {
    buildId: number,
    submissionId: number
}

type BuildCommandStatus = {
    commandLine: string,
    state: 'RUNNING' | 'FINISHED' | 'WAITING',
    output: string | null
}

type BuildStatus = {
    request: BuildRequest,
    descriptor: string,
    commands: BuildCommandStatus[],
    startTime: number
}