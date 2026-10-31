export interface Toast {
  id: number
  text: string
  tone: 'info' | 'ok' | 'error'
}

let seq = 0

/** 화면 아래에 잠깐 떴다 사라지는 알림 */
export function useToast() {
  const toasts = useState<Toast[]>('toasts', () => [])

  function show(text: string, tone: Toast['tone'] = 'info', ms = 2600) {
    const id = ++seq
    toasts.value = [...toasts.value.slice(-2), { id, text, tone }]
    if (import.meta.client) setTimeout(() => dismiss(id), ms)
  }

  function dismiss(id: number) {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  return { toasts, show, dismiss }
}
