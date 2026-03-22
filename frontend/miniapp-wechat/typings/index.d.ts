interface IAppOption {
  globalData: {
    accessToken: string
    refreshToken: string
    currentRoomId: number
    selectedBillRoomId?: number
    agentContextPrompt?: string
    appName?: string
  }
}
