export function formatLocalDateTime(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

export function buildActiveExamWindow() {
  const now = new Date()
  const start = new Date(now.getTime() - 10 * 60_000)
  const end = new Date(now.getTime() + 3 * 60 * 60_000)
  return {
    startTime: formatLocalDateTime(start),
    endTime: formatLocalDateTime(end)
  }
}
