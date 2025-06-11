<template>
  <!-- マウスが動くたび onMouseMove 関数が呼ばれる -->
  <!-- マウスボタンが押されたら onMouseDown 関数が呼ばれる -->
  <!-- マウスホイールで onMouseWeel 関数が呼ばれる -->
  <div
    @mousemove="onMouseMove"
    @mousedown="onMouseDown"
    @wheel="onMouseWheel"
    :class="{ 'cursor-hidden': cursorHidden }"
    style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 9999"
  >
    <h1>🖱️ WebSocket Mouse Sender</h1>
    <!-- ボタンクリックで sendMessage 関数を呼び出す -->
    <button @click="sendMessage">送信（手動）</button>
    <!-- 通信ログを画面に表示 -->
    <pre>{{ log }}</pre>
  </div>
</template>

<script setup>
// Vueの機能をimport（ref:リアクティブ変数, onMounted:マウント時の処理）
import { ref, onMounted } from 'vue'

const cursorHidden = ref(false) // カーソル非表示状態（追加）
const isInputEnabled = ref(true) //一時停止フラグ

// リアクティブなログ変数
const log = ref('')

// WebSocket本体、マウス座標用の変数
let ws
let lastX = null
let lastY = null

let screenWidth = window.innerWidth // 画面の幅
let screenHeight = window.innerHeight // 画面の高さ
const warpThreshold = 20 // 5px以内ならワープ判定

const clientId = 'PC-A'
const isMac = clientId === 'PC-A'

const warpCooldownMs = 500
let lastWarpSentTime = 0

// コンポーネントが画面に表示されたらWebSocket接続開始（再接続導入）
onMounted(() => {
  connectWebSocket()
})

function connectWebSocket() {
  ws = new WebSocket(import.meta.env.VITE_WS_URL)

  ws.onopen = () => {
    log.value += '✅ 接続成功\n'
    safeSend({
      type: 'register',
      from: 'PC-A',
      to: 'PC-B',
      dummy: true,
    })
  }

  ws.onmessage = (msg) => {
    log.value += '📩 受信 raw: ' + msg.data + '\n'

    try {
      const data = JSON.parse(msg.data)

      if (data.type === 'warp' && data.to && data.from) {
        console.log('📌 ワープ: from', data.from, 'to', data.to)

        if (!isMac) return // Windows側は入力制御しない

        if (data.to === clientId) {
          isInputEnabled.value = true
          cursorHidden.value = false
          log.value += `🔛 入力有効（戻ってきた）: ${clientId}\n`
        } else if (data.from === clientId) {
          isInputEnabled.value = false
          cursorHidden.value = true
          log.value += `⛔ 入力無効（離れた）: ${clientId}\n`
        }
      }
    } catch (e) {
      log.value += '⚠️ JSONパース失敗: ' + e.message + '\n'
    }
  }

  ws.onerror = (e) => {
    log.value += '❌ 接続エラー: ' + e.message + '\n'
  }

  ws.onclose = () => {
    log.value += '🔌 切断されました。再接続します...\n'
    setTimeout(connectWebSocket, 3000) // 3秒後に再接続
  }
}
function safeSend(message) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message))
  } else {
    log.value += '⚠️ WebSocketが開いていないため送信できませんでした\n'
  }
}

// 手動でメッセージを送信する関数
function sendMessage() {
  safeSend({
    type: 'move',
    from: 'PC-A',
    to: 'PC-B',
    deltaX: 5,
    deltaY: 5,
  })
  log.value += '📤 テスト送信: PC-A → PC-B\n'
}

// 🎯 マウスが動いたとき呼ばれる関数
function onMouseMove(event) {
  const { clientX, clientY } = event
  console.log(`🖱 clientY=${clientY}, 判定値=${screenHeight - warpThreshold}`) //TODO

  if (clientId === 'PC-A' && isInputEnabled.value) {
    //if (clientId === 'PC-A') {
    if (lastX !== null && lastY !== null) {
      const deltaX = clientX - lastX
      const deltaY = clientY - lastY

      if (Math.abs(deltaX) > 0 || Math.abs(deltaY) > 0) {
        safeSend({
          type: 'move',
          from: 'PC-A',
          to: 'PC-B',
          deltaX,
          deltaY,
        })
        log.value += `📤 移動送信: dx=${deltaX}, dy=${deltaY}\n`
      }
    }
  }

  // 🛸 warp 判定
  if (clientId === 'PC-A') {
    if (clientX <= warpThreshold) {
      sendWarp('left')
    } else if (clientX >= screenWidth - warpThreshold) {
      sendWarp('right')
    }
    // 上下は無効化（何もしない）
    //} else if (clientId === 'PC-B') {
  } else if (clientId !== 'PC-A') {
    if (clientY >= screenHeight - warpThreshold) {
      sendWarp('down')
    }
    // 左右は無効化（何もしない）
  }

  lastX = clientX
  lastY = clientY
}

function sendWarp(direction) {
  if (!direction || typeof direction !== 'string') return
  const now = Date.now()
  if (now - lastWarpSentTime < warpCooldownMs) {
    return // クールダウン中なので送信しない
  }
  lastWarpSentTime = now
  safeSend({
    type: 'warp',
    from: clientId,
    to: 'PC-B',
    direction,
  })
  log.value += `📤 ワープ指示送信: direction=${direction}\n`
}
//クリック送信
function onMouseDown(event) {
  safeSend({
    type: 'click',
    from: clientId,
    to: 'PC-B',
    button: event.button,
  })
  log.value += `📤 クリック送信: button=${event.button}\n`
}

//wheelイベント検知、メッセージ送信
function onMouseWheel(event) {
  safeSend({
    type: 'scroll',
    from: clientId,
    to: 'PC-B',
    deltaY: event.deltaY,
  })
  log.value += `📤 スクロール送信: deltaY=${event.deltaY}\n`
}
</script>

<style scoped>
.cursor-hidden {
  cursor: none;
}
</style>
