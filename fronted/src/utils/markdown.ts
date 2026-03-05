function escapeHtml(input: string) {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function renderInline(text: string) {
  return text
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
}

export function renderMarkdown(source: string) {
  const escaped = escapeHtml(source)
  const lines = escaped.replace(/\r\n/g, '\n').split('\n')

  const html: string[] = []
  let inCodeBlock = false
  let inUnorderedList = false
  let inOrderedList = false
  let inBlockquote = false

  const closeBlocks = () => {
    if (inUnorderedList) {
      html.push('</ul>')
      inUnorderedList = false
    }
    if (inOrderedList) {
      html.push('</ol>')
      inOrderedList = false
    }
    if (inBlockquote) {
      html.push('</blockquote>')
      inBlockquote = false
    }
  }

  for (const line of lines) {
    const trimmed = line.trim()

    if (trimmed.startsWith('```')) {
      closeBlocks()
      if (!inCodeBlock) {
        inCodeBlock = true
        html.push('<pre><code>')
      } else {
        inCodeBlock = false
        html.push('</code></pre>')
      }
      continue
    }

    if (inCodeBlock) {
      html.push(`${line}\n`)
      continue
    }

    if (trimmed === '') {
      closeBlocks()
      html.push('<br />')
      continue
    }

    const orderedMatch = trimmed.match(/^(\d+)\.\s+(.+)/)
    if (orderedMatch) {
      if (!inOrderedList) {
        closeBlocks()
        inOrderedList = true
        html.push('<ol>')
      }
      html.push(`<li>${renderInline(orderedMatch[2] ?? '')}</li>`)
      continue
    }

    if (/^[-*+]\s+/.test(trimmed)) {
      if (!inUnorderedList) {
        closeBlocks()
        inUnorderedList = true
        html.push('<ul>')
      }
      html.push(`<li>${renderInline(trimmed.replace(/^[-*+]\s+/, ''))}</li>`)
      continue
    }

    if (trimmed.startsWith('>')) {
      if (!inBlockquote) {
        closeBlocks()
        inBlockquote = true
        html.push('<blockquote>')
      }
      html.push(`<p>${renderInline(trimmed.replace(/^>\s?/, ''))}</p>`)
      continue
    }

    closeBlocks()

    if (trimmed.startsWith('### ')) {
      html.push(`<h3>${renderInline(trimmed.slice(4))}</h3>`)
      continue
    }
    if (trimmed.startsWith('## ')) {
      html.push(`<h2>${renderInline(trimmed.slice(3))}</h2>`)
      continue
    }
    if (trimmed.startsWith('# ')) {
      html.push(`<h1>${renderInline(trimmed.slice(2))}</h1>`)
      continue
    }

    html.push(`<p>${renderInline(trimmed)}</p>`)
  }

  closeBlocks()
  if (inCodeBlock) {
    html.push('</code></pre>')
  }

  return html.join('')
}
