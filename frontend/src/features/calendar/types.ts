// Shapes of the calendar API (GET /api/me/calendar, /api/me/tasks, D51, D117, D118)

export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export type EventType = 'COMBAT_STAGES' | 'DOUBLE_DROP' | 'POINTS_SHOP'

export type FarmingSourceType = 'SIMULATION_TRAINING' | 'BOSS_CHALLENGE' | 'OVERWORLD_ENEMIES' | 'OVERWORLD_COLLECTION'

export interface EventActivity {
  title: string
  minutes: number
  frequency: 'DAILY' | 'ONCE'
  // The day of the edition from which it is available; null = from the first (D96)
  unlockDay: number | null
}

export interface EventEffect {
  type: 'DROP_MULTIPLIER'
  sourceType: FarmingSourceType
  multiplier: number
  // null = every run of the day (D94)
  dailyLimit: number | null
}

/** One edition of an event (D113): the whole dates, and the days of the range on which it is active (D96). */
export interface GameEvent {
  id: number
  name: string
  type: EventType
  description: string
  startDate: string
  endDate: string
  // null = every day
  activeWeekdays: Weekday[] | null
  activeDates: string[]
  rewards: string[]
  activities: EventActivity[]
  effect: EventEffect | null
}

export type TaskOrigin = 'AGENT' | 'MANUAL'
export type TaskKind = 'FARMING' | 'EVENT' | 'CUSTOM'
export type TaskStatus = 'PROPOSED' | 'CONFIRMED' | 'DONE' | 'REJECTED' | 'REPLACED'

export interface CalendarTask {
  id: number
  origin: TaskOrigin
  kind: TaskKind
  status: TaskStatus
  taskDate: string
  title: string
  notes: string | null
  minutes: number
  farmingSourceId: string | null
  runs: number | null
  // Runs × cost of the source, computed by the server; 0 for the other kinds
  waveplates: number
  eventId: number | null
  characterId: string | null
  completedAt: string | null
}

export type ProposalStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'REPLACED'

/** A plan of the planner that waits for the user (D22, D124); a rejected or replaced one has no tasks. */
export interface Proposal {
  id: number
  status: ProposalStatus
  summary: string | null
  createdAt: string
  tasks: CalendarTask[]
}

export interface CalendarData {
  events: GameEvent[]
  // CONFIRMED and DONE: the tasks of the pending proposal come apart, all of them (D124)
  tasks: CalendarTask[]
  pendingProposal: Proposal | null
  // Roster, goals or events of the last plan changed after it was made (D23, D125)
  planOutdated: boolean
}

/** The days shown by the calendar, both included, as "2026-10-12". */
export interface DateRange {
  from: string
  to: string
}

/** The form of a manual task (D117): the minutes are sent as a number, the empty notes as empty. */
export interface TaskForm {
  title: string
  taskDate: string
  minutes: number
  notes: string
}

export interface AddedMaterial {
  materialId: string
  quantity: number
}

/** A task marked as done and what its runs gave (D118). */
export interface Completion {
  task: CalendarTask
  addedToInventory: AddedMaterial[]
}
