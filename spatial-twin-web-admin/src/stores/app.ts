import { defineStore } from 'pinia'

interface ProjectSummary {
  id: string
  name: string
  buildingCount: number
  detailBuildingCount: number
  status: '草稿' | '已发布'
}

interface AppState {
  currentProjectId: string
  projects: ProjectSummary[]
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    currentProjectId: 'project-demo-campus',
    projects: [
      {
        id: 'project-demo-campus',
        name: '示范科技园区',
        buildingCount: 5,
        detailBuildingCount: 1,
        status: '草稿',
      },
      {
        id: 'project-demo-community',
        name: '智慧住宅样板',
        buildingCount: 8,
        detailBuildingCount: 2,
        status: '已发布',
      },
    ],
  }),
  getters: {
    currentProject(state): ProjectSummary | undefined {
      return state.projects.find((project) => project.id === state.currentProjectId)
    },
  },
})
