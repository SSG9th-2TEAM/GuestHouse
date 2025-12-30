<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { createChatRoom, getChatRooms, getRoomMessages, sendChatbotMessage, deleteChatRoom } from '@/api/chatbotClient'

const isOpen = ref(false)
const viewMode = ref('list') // 'list' | 'chat'
const chatRooms = ref([])
const messages = ref([])
const currentRoomId = ref(null)
const isLoading = ref(false)
const chatContainer = ref(null)

// Modal State
const showModal = ref(false)
const modalState = ref({
  title: '',
  message: '',
  type: 'confirm', // 'confirm' | 'alert'
  onConfirm: null
})

const closeModal = () => { showModal.value = false }

const openConfirm = (title, message, onConfirm) => {
    modalState.value = { title, message, type: 'confirm', onConfirm }
    showModal.value = true
}

const openAlert = (title, message) => {
    modalState.value = { title, message, type: 'alert', onConfirm: null }
    showModal.value = true
}

const onModalConfirm = () => {
    if (modalState.value.onConfirm) modalState.value.onConfirm()
    closeModal()
}

// 메뉴 카테고리 버튼
const menuCategories = [
  { emoji: '📅', label: '예약 문의' },
  { emoji: '💳', label: '결제 문의' },
  { emoji: '💰', label: '환불 문의' },
  { emoji: '👤', label: '계정/로그인' },
  { emoji: '⭐', label: '후기/리뷰' },
  { emoji: '🚨', label: '문제/신고' },
  { emoji: '🏠', label: '호스트 센터' }
]

// 시간 포맷
const formatTime = (dateInput) => {
  if (!dateInput) return ''
  const date = new Date(dateInput)
  const now = new Date()
  
  // 오늘이면 시간만, 아니면 날짜
  const isToday = date.getDate() === now.getDate() && 
                 date.getMonth() === now.getMonth() && 
                 date.getFullYear() === now.getFullYear()

  if (isToday) {
      const hours = date.getHours()
      const minutes = date.getMinutes().toString().padStart(2, '0')
      const period = hours >= 12 ? '오후' : '오전'
      const displayHours = hours > 12 ? hours - 12 : hours
      return `${period} ${displayHours}:${minutes}`
  } else {
      return `${date.getMonth() + 1}월 ${date.getDate()}일`
  }
}

const getCurrentTime = () => formatTime(new Date())

// 텍스트에서 번호 옵션 추출
const parseNumberedOptions = (text) => {
  const options = []
  const regex = /(\d+)[.\)]\s*([^\n]+)/g
  let match
  while ((match = regex.exec(text)) !== null) {
    options.push({
      number: match[1],
      label: match[2].trim(),
      fullText: match[0].trim()
    })
  }
  return options
}

const getCleanBody = (text) => {
  return text.replace(/(\d+)[.\)]\s*([^\n]+)/g, '').trim()
}

// Toggle Chat Window
const toggleChat = async () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
      viewMode.value = 'list' // 항상 리스트로 시작
      await loadRooms()
  }
}

// Load Rooms
const loadRooms = async () => {
    isLoading.value = true
    try {
        const res = await getChatRooms()
        if (res.ok && res.data) {
            chatRooms.value = res.data
        }
    } catch (e) {
        console.error(e)
    } finally {
        isLoading.value = false
    }
}

// Start New Chat
const startNewChat = async () => {
    isLoading.value = true
    try {
        const res = await createChatRoom()
        if (res.ok && res.data && res.data.roomId) {
            await enterRoom(res.data.roomId)
        }
    } catch(e) {
        console.error(e)
    } finally {
        isLoading.value = false
    }
}

const deleteRoom = (roomId, event) => {
    event.stopPropagation()
    
    openConfirm('대화 삭제', '정말 이 대화를 삭제하시겠습니까?', async () => {
        try {
            const res = await deleteChatRoom(roomId)
            if (res.ok) {
                await loadRooms()
                if (currentRoomId.value === roomId) {
                     goBackToList()
                }
            } else {
                openAlert('오류', '삭제에 실패했습니다.')
            }
        } catch(e) {
            console.error(e)
            openAlert('오류', '오류가 발생했습니다.')
        }
    })
}

// Enter Room
const enterRoom = async (roomId) => {
    currentRoomId.value = roomId
    messages.value = []
    viewMode.value = 'chat'
    isLoading.value = true
    
    try {
        const res = await getRoomMessages(roomId)
        if (res.ok && res.data && res.data.length > 0) {
             for (const item of res.data) {
                const timeStr = formatTime(item.createdAt)
                if (item.fromBot) {
                    const extractedOptions = parseNumberedOptions(item.message)
                    const cleanBody = getCleanBody(item.message)
                    
                    const isWelcome = item.message.includes('도와드릴까요')
                    
                    messages.value.push({
                        type: 'bot',
                        text: cleanBody,
                        time: timeStr,
                        options: extractedOptions,
                        showMenu: isWelcome, 
                        showHome: !isWelcome
                    })
                } else {
                    messages.value.push({
                        type: 'user',
                        text: item.message,
                        time: timeStr
                    })
                }
            }
        } else {
            // 메시지가 없으면 클라이언트에서 웰컴 메시지 강제 표시 (Fallback)
             messages.value.push({
                type: 'bot',
                text: '안녕하세요 지금이곳 FAQ 챗봇 입니다. 무엇을 도와드릴까요?',
                time: getCurrentTime(),
                showMenu: true,
                showHome: false
            })
        }
    } catch(e) {
        console.error(e)
    } finally {
        isLoading.value = false
        await nextTick()
        scrollToBottom()
    }
}

const goBackToList = () => {
    viewMode.value = 'list'
    currentRoomId.value = null
    loadRooms()
}

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// Send Message
const sendUserMessage = async (message) => {
  if (!message || isLoading.value || !currentRoomId.value) return

  // UI update
  messages.value.push({
    type: 'user',
    text: message,
    time: getCurrentTime()
  })
  
  isLoading.value = true
  await nextTick()
  scrollToBottom()

  try {
      const res = await sendChatbotMessage(currentRoomId.value, message)
      if (res.ok && res.data) {
          const text = res.data.reply
          const options = parseNumberedOptions(text)
          const cleanBody = getCleanBody(text)
          const isWelcome = text.includes('도와드릴까요')
          
          messages.value.push({
              type: 'bot',
              text: cleanBody,
              options: options,
              time: getCurrentTime(),
              showMenu: isWelcome,
              showHome: !isWelcome
          })
      } else {
          // Error handling
          messages.value.push({
              type: 'bot',
              text: '죄송합니다. 답변을 불러오지 못했습니다.',
              time: getCurrentTime()
          })
      }
  } catch(e) {
      console.error(e)
      messages.value.push({
          type: 'bot',
          text: '오류가 발생했습니다.',
          time: getCurrentTime()
      })
  } finally {
      isLoading.value = false
      await nextTick()
      scrollToBottom()
  }
}

const selectOption = (label) => {
    sendUserMessage(label)
}

const goHome = () => {
    // 처음으로: 여기서는 새 문의하기가 아니라, 현재 대화방에서 처음으로 돌아가는 것?
    // 보통 "처음으로"는 웰컴 메시지를 다시 띄우는 것.
    // 그렇다면 그냥 sendUserMessage('처음으로') 처리하면 백엔드에서 웰컴을 주나?
    // 백엔드 FAQ에 "처음으로" 처리가 있는지 모름.
    // 프론트에서 처리: 웰컴 메시지 강제 표시
    
    messages.value.push({
        type: 'user',
        text: '처음으로',
        time: getCurrentTime()
    })
    
    setTimeout(() => {
         messages.value.push({
            type: 'bot',
            text: '무엇을 도와드릴까요?',
            time: getCurrentTime(),
            showMenu: true,
            showHome: false
        })
        scrollToBottom()
    }, 500)
}

</script>

<template>
  <div class="chatbot-wrapper">
    <!-- Launcher -->
    <button 
      class="chat-launcher" 
      @click="toggleChat"
      :class="{ 'is-open': isOpen }"
    >
      <img src="/icon.png" alt="Chat" v-if="!isOpen" />
      <span v-else class="close-icon">✕</span>
    </button>

    <!-- Window -->
    <div v-if="isOpen" class="chatbot-window">
      
      <!-- Header -->
      <div class="chatbot-header">
         <button v-if="viewMode === 'chat'" @click="goBackToList" class="back-btn">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M15 18L9 12L15 6" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
         </button>
         <span class="header-title">{{ viewMode === 'list' ? '대화' : '지금이곳' }}</span>
         <button class="close-btn" @click="toggleChat">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M18 6L6 18M6 6L18 18" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
         </button>
      </div>

      <!-- List View -->
      <div v-if="viewMode === 'list'" class="room-list-container">
          <div v-if="isLoading && chatRooms.length === 0" class="loading-bubble">
              <span></span><span></span><span></span>
          </div>
          <div v-else-if="chatRooms.length === 0" class="empty-state">
              <p>문의 내역이 없습니다.</p>
              <p class="sub-text">새 문의하기를 눌러 대화를 시작하세요.</p>
          </div>
          <div v-else class="room-list">
              <div v-for="room in chatRooms" :key="room.id" class="room-item" @click="enterRoom(room.id)">
                  <div class="room-icon">
                      <img src="/icon.png" alt="Bot" />
                  </div>
                  <div class="room-info">
                      <div class="room-sender">지금이곳</div>
                      <div class="room-msg">{{ room.lastMessage }}</div>
                      <div class="room-meta">
                          <span class="room-time">{{ formatTime(room.updatedAt) }}</span>
                      </div>
                  </div>
                  <button class="room-delete-btn" @click="(e) => deleteRoom(room.id, e)">✕</button>
              </div>
          </div>
          
          <div class="new-chat-container">
              <button class="new-chat-btn" @click="startNewChat">
                  새 문의하기 ▼
              </button>
          </div>
      </div>

      <!-- Chat View -->
      <div v-else class="chat-container" ref="chatContainer">
        <div class="messages-area">
            <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.type">
                
                <div v-if="msg.type === 'bot'" class="bot-container">
                    <div class="bot-profile">
                        <img src="/icon.png" alt="Bot" />
                    </div>
                    <div class="bot-content">
                        <div class="bot-name">지금이곳</div>
                        
                        <!-- Text Bubble -->
                        <div class="message-bubble bot-bubble">
                            <div class="message-text" style="white-space: pre-wrap;">{{ msg.text }}</div>
                        </div>

                        <!-- Menu (Welcome only) -->
                        <div v-if="msg.showMenu" class="menu-grid">
                            <button 
                                v-for="(cat, idx) in menuCategories" 
                                :key="idx" 
                                class="menu-btn"
                                @click="selectOption(cat.label)"
                            >
                                <span class="menu-emoji">{{ cat.emoji }}</span>
                                <span class="menu-label">{{ cat.label }}</span>
                            </button>
                        </div>

                        <!-- Option Buttons -->
                        <div v-if="msg.options && msg.options.length > 0" class="options-list">
                             <button 
                                v-for="(opt, idx) in msg.options" 
                                :key="idx" 
                                class="option-btn"
                                @click="selectOption(opt.fullText)"
                             >
                                {{ opt.fullText }}
                             </button>
                        </div>
                        
                        <!-- Home Button -->
                        <div v-if="msg.showHome" class="home-btn-container">
                            <button class="home-btn" @click="goHome">처음으로</button>
                        </div>
                        
                        <div class="message-time">{{ msg.time }}</div>
                    </div>
                </div>

                <div v-else class="user-container">
                    <div class="message-bubble user-bubble">
                        {{ msg.text }}
                    </div>
                    <div class="message-time">{{ msg.time }}</div>
                </div>

            </div>
            
            <div v-if="isLoading" class="loading-bubble">
                <span>.</span><span>.</span><span>.</span>
            </div>
        </div>
      </div>

      <!-- Modal Overlay -->
      <div v-if="showModal" class="modal-overlay">
          <div class="modal-content">
              <div class="modal-title">{{ modalState.title }}</div>
              <div class="modal-desc">{{ modalState.message }}</div>
              <div class="modal-actions">
                  <button v-if="modalState.type === 'confirm'" class="modal-btn cancel" @click="closeModal">취소</button>
                  <button class="modal-btn confirm" @click="onModalConfirm">확인</button>
              </div>
          </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.chatbot-wrapper {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 9999;
  font-family: 'Pretendard', sans-serif;
}

.chat-launcher {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: white;
  border: 1px solid #ddd;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
}
.chat-launcher:hover { transform: scale(1.05); }
.chat-launcher img { width: 32px; height: 32px; }
.close-icon { font-size: 24px; color: #333; }

.chatbot-window {
  position: absolute;
  bottom: 80px;
  right: 0;
  width: 380px;
  height: 600px;
  max-height: calc(100vh - 120px);
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #eee;
}

/* Header */
.chatbot-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}
.header-title {
  font-weight: 700;
  font-size: 18px;
}
.back-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}
.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: none; /* 기본적으로 숨김 - 모바일에서만 표시 */
  align-items: center;
}

/* Room List */
.room-list-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: #fff;
    overflow-y: auto;
}
.room-list {
    padding: 10px 0;
    padding-bottom: 80px; /* Space for fixed button */
}
.room-item {
    display: flex;
    padding: 16px 20px;
    cursor: pointer;
    transition: background 0.2s;
    border-bottom: 1px solid #f9f9f9;
}
.room-item:hover { background: #f9f9f9; }
.room-item:hover .room-delete-btn { opacity: 1; }
.room-icon img {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    margin-right: 16px;
    border: 1px solid #eee;
}
.room-info {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    justify-content: center;
}
.room-sender {
    font-weight: 700;
    font-size: 15px;
    margin-bottom: 4px;
}
.room-msg {
    font-size: 14px;
    color: #666;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
}
.room-time {
    font-size: 12px;
    color: #999;
}
.empty-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #999;
}
.new-chat-container {
    padding: 20px;
    text-align: center;
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: linear-gradient(to top, #fff 80%, transparent);
}
.new-chat-btn {
    background: #0f766e; /* brand-accent */
    color: white;
    border: none;
    padding: 12px 24px;
    border-radius: 30px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(15, 118, 110, 0.3);
    display: inline-flex;
    align-items: center;
    gap: 8px;
    transition: background 0.2s;
}
.new-chat-btn:hover { background: #0d6e66; }

.room-delete-btn {
    opacity: 0;
    background: none;
    border: none;
    color: #999;
    font-size: 16px;
    cursor: pointer;
    padding: 0 8px;
    transition: opacity 0.2s, color 0.2s;
}
.room-delete-btn:hover { color: #ff4444; }

/* Chat View */
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fff;
  display: flex;
  flex-direction: column;
}
.bot-container {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.bot-profile img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid #eee;
}
.bot-content {
  flex: 1;
  max-width: 80%;
}
.bot-name {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 6px;
  color: #333;
}
.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
}
.bot-bubble {
  background: #f2f2f2;
  border-top-left-radius: 4px;
  color: #333;
}
.user-container {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-bottom: 20px;
}
.user-bubble {
  background: #333;
  color: white;
  border-bottom-right-radius: 4px;
  max-width: 80%;
}
.message-time {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
}

/* Menu Grid */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 10px;
}
.menu-btn {
  background: white;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.menu-btn:hover { border-color: #333; transform: translateY(-2px); }
.menu-emoji { font-size: 20px; }
.menu-label { font-size: 13px; font-weight: 500; }

/* Options List */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}
.option-btn {
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 10px 16px;
  text-align: left;
  font-size: 14px;
  cursor: pointer;
  color: #333;
  transition: all 0.2s;
}
.option-btn:hover {
  background: #f9f9f9;
  border-color: #999;
}

.home-btn-container { margin-top: 10px; }
.home-btn {
  background: white;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 10px 16px;
  text-align: center;
  font-size: 14px;
  cursor: pointer;
  color: #333;
  width: 100%;
  transition: all 0.2s;
}
.home-btn:hover {
  background: #f9f9f9;
  border-color: #999;
}

/* Modal */
.modal-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  border-radius: 20px; /* Window defaults */
}
.modal-content {
  background: white;
  padding: 24px;
  border-radius: 16px;
  width: 80%;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  animation: modalPop 0.2s ease-out;
}
@keyframes modalPop {
    from { transform: scale(0.9); opacity: 0; }
    to { transform: scale(1); opacity: 1; }
}
.modal-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 8px;
  color: #333;
}
.modal-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
  white-space: pre-wrap;
}
.modal-actions {
  display: flex;
  gap: 10px;
}
.modal-btn {
  flex: 1;
  padding: 12px 0;
  border-radius: 12px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.modal-btn.cancel {
  background: #f5f5f5;
  color: #666;
}
.modal-btn.confirm {
  background: #333;
  color: white;
}
.modal-btn:hover { opacity: 0.9; }

/* Mobile */
@media (max-width: 480px) {
  .chatbot-wrapper {
    bottom: 16px;
    right: 16px;
  }
  .chat-launcher {
    width: 56px;
    height: 56px;
  }
  .chat-launcher img {
    width: 28px;
    height: 28px;
  }
  .close-btn {
    display: flex; /* 모바일에서 닫기 버튼 표시 */
  }
  .chatbot-window {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    bottom: 0;
    right: 0;
    border-radius: 0;
    max-height: none;
  }
}

/* Loading Bubble */
.loading-bubble {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 6px;
    padding: 20px;
    margin: 0 auto;
}
.loading-bubble span {
    width: 6px;
    height: 6px;
    background: #333;
    border-radius: 50%;
    display: inline-block;
    animation: bounce 1.4s infinite ease-in-out both;
}
.loading-bubble span:nth-child(1) { animation-delay: -0.32s; }
.loading-bubble span:nth-child(2) { animation-delay: -0.16s; }
.loading-bubble span:nth-child(3) { color: transparent; /* Hide text dot */ }

@keyframes bounce {
    0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
    40% { transform: scale(1); opacity: 1; }
}
</style>
