<script setup>
import { ref } from 'vue'

const emit = defineEmits(['cancel', 'submit'])

const form = ref({
  name: '',
  location: '',
  price: '',
  maxGuests: 2,
  roomCount: 1,
  description: '',
  images: []
})

const handleImageUpload = (event) => {
  const files = Array.from(event.target.files)
  // Mock image upload - just creating object URLs for preview
  const newImages = files.map(file => URL.createObjectURL(file))
  form.value.images = [...form.value.images, ...newImages]
}

const removeImage = (index) => {
  form.value.images.splice(index, 1)
}

const handleSubmit = () => {
  // Validate form
  if (!form.value.name || !form.value.location || !form.value.price) {
    alert('필수 정보를 모두 입력해주세요.')
    return
  }
  
  emit('submit', { ...form.value })
}
</script>

<template>
  <div class="register-container">
    <div class="header">
      <button class="back-btn" @click="$emit('cancel')">
        <span class="icon">←</span> 뒤로가기
      </button>
      <h2>새 숙소 등록</h2>
    </div>

    <div class="form-container">
      <!-- Basic Info -->
      <section class="form-section">
        <h3>기본 정보</h3>
        <div class="form-group">
          <label>숙소 이름</label>
          <input v-model="form.name" type="text" placeholder="숙소의 매력을 나타내는 이름을 지어주세요" />
        </div>
        <div class="form-group">
          <label>숙소 위치</label>
          <input v-model="form.location" type="text" placeholder="도로명 주소 입력" />
        </div>
        <div class="form-group">
          <label>1박 가격 (₩)</label>
          <input v-model="form.price" type="number" placeholder="0" />
        </div>
        <div class="form-group">
          <label>숙소 설명</label>
          <textarea v-model="form.description" rows="4" placeholder="숙소에 대한 자세한 설명을 적어주세요"></textarea>
        </div>
      </section>

      <!-- Capacity Info -->
      <section class="form-section">
        <h3>수용 인원 및 객실</h3>
        <div class="row">
          <div class="form-group half">
            <label>최대 인원</label>
            <div class="counter-input">
              <button @click="form.maxGuests > 1 && form.maxGuests--" type="button">-</button>
              <span>{{ form.maxGuests }}명</span>
              <button @click="form.maxGuests++" type="button">+</button>
            </div>
          </div>
          <div class="form-group half">
            <label>객실 수</label>
            <div class="counter-input">
              <button @click="form.roomCount > 1 && form.roomCount--" type="button">-</button>
              <span>{{ form.roomCount }}개</span>
              <button @click="form.roomCount++" type="button">+</button>
            </div>
          </div>
        </div>
      </section>

      <!-- Image Upload -->
      <section class="form-section">
        <h3>숙소 사진</h3>
        <div class="image-upload-area">
          <div class="upload-btn-wrapper">
            <button class="upload-btn">
              <span class="icon">📷</span>
              사진 추가하기
            </button>
            <input type="file" multiple accept="image/*" @change="handleImageUpload" />
          </div>
          <p class="help-text">최대 10장까지 업로드 가능합니다.</p>
        </div>

        <div v-if="form.images.length > 0" class="image-preview-grid">
          <div v-for="(img, idx) in form.images" :key="idx" class="image-preview">
            <img :src="img" />
            <button class="remove-btn" @click="removeImage(idx)">×</button>
          </div>
        </div>
      </section>

      <!-- Actions -->
      <div class="form-actions">
        <button class="cancel-btn" @click="$emit('cancel')">취소</button>
        <button class="submit-btn" @click="handleSubmit">등록 완료</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 4rem;
}

.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1rem;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0;
}

.back-btn:hover {
  color: #222;
}

.header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}

.form-section {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  margin-bottom: 1.5rem;
}

.form-section h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  color: #222;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  font-size: 0.9rem;
  font-weight: 500;
  color: #444;
  margin-bottom: 0.5rem;
}

input[type="text"],
input[type="number"],
textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: #BFE7DF;
  box-shadow: 0 0 0 3px rgba(191, 231, 223, 0.2);
}

.row {
  display: flex;
  gap: 1rem;
}

.half {
  flex: 1;
}

.counter-input {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: #f8f9fa;
  padding: 0.5rem;
  border-radius: 8px;
  width: fit-content;
}

.counter-input button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-weight: 600;
}

.counter-input button:hover {
  border-color: #BFE7DF;
  color: #004d40;
}

.counter-input span {
  font-weight: 600;
  min-width: 40px;
  text-align: center;
}

/* Image Upload */
.image-upload-area {
  margin-bottom: 1.5rem;
}

.upload-btn-wrapper {
  position: relative;
  overflow: hidden;
  display: inline-block;
}

.upload-btn {
  padding: 0.75rem 1.5rem;
  border: 1px dashed #BFE7DF;
  background: #f5fcfb;
  color: #004d40;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.upload-btn-wrapper input[type=file] {
  font-size: 100px;
  position: absolute;
  left: 0;
  top: 0;
  opacity: 0;
  cursor: pointer;
}

.help-text {
  font-size: 0.85rem;
  color: #888;
  margin-top: 0.5rem;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 1rem;
}

.image-preview {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  line-height: 1;
}

/* Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}

.cancel-btn {
  padding: 1rem 2rem;
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  color: #666;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn {
  padding: 1rem 2rem;
  background: #BFE7DF;
  border: none;
  border-radius: 8px;
  color: #004d40;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn:hover {
  background: #a8ddd2;
}
</style>
