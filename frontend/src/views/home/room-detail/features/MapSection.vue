<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'

const props = defineProps({
  latitude: {
    type: [Number, String],
    default: null
  },
  longitude: {
    type: [Number, String],
    default: null
  },
  address: {
    type: String,
    default: ''
  },
  transportInfo: {
    type: String,
    default: ''
  }
})

const kakaoMapRef = ref(null)
const mapError = ref(false)
const resolvedAddressCoords = ref(null)

const mapAddress = computed(() => (props.address || '').trim())
const hasCoordinates = computed(() => {
  const latitude = props.latitude
  const longitude = props.longitude
  if (latitude == null || longitude == null || latitude === '' || longitude === '') return false
  return Number.isFinite(Number(latitude)) && Number.isFinite(Number(longitude))
})
const hasMapLocation = computed(() => hasCoordinates.value || mapAddress.value.length > 0)
const transportText = computed(() => props.transportInfo || '교통 정보가 없습니다.')

let kakaoMap = null
let kakaoMarker = null
let kakaoMapPromise = null

const loadKakaoMap = () => {
  if (window.kakao?.maps?.load) {
    return Promise.resolve()
  }
  if (kakaoMapPromise) return kakaoMapPromise

  kakaoMapPromise = new Promise((resolve, reject) => {
    const loadMaps = () => {
      if (window.kakao?.maps?.load) {
        resolve()
        return
      }
      reject(new Error('Kakao map sdk not ready'))
    }

    if (window.kakao?.maps) {
      loadMaps()
      return
    }

    const appKey = 'edff5897f4250925f0de3f4ca22d615f'
    if (!appKey) {
      reject(new Error('Kakao map key is missing'))
      return
    }

    const script = document.createElement('script')
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&libraries=services&autoload=false`
    script.async = true
    script.onload = loadMaps
    script.onerror = reject
    document.head.appendChild(script)
  })

  return kakaoMapPromise
}

const resolveMapCenter = async () => {
  const rawLatitude = props.latitude
  const rawLongitude = props.longitude
  if (rawLatitude != null && rawLongitude != null && rawLatitude !== '' && rawLongitude !== '') {
    const latitude = Number(rawLatitude)
    const longitude = Number(rawLongitude)
    if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
      return { latitude, longitude }
    }
  }

  if (!mapAddress.value) return null
  if (resolvedAddressCoords.value) return resolvedAddressCoords.value
  if (!window.kakao?.maps?.services?.Geocoder) return null

  const geocoder = new window.kakao.maps.services.Geocoder()
  const coords = await new Promise((resolve) => {
    geocoder.addressSearch(mapAddress.value, (result, status) => {
      if (status === window.kakao.maps.services.Status.OK && Array.isArray(result) && result.length) {
        const latitude = Number(result[0].y)
        const longitude = Number(result[0].x)
        if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
          resolve({ latitude, longitude })
          return
        }
      }
      resolve(null)
    })
  })

  if (coords) {
    resolvedAddressCoords.value = coords
  }

  return coords
}

const renderKakaoMap = async () => {
  mapError.value = false
  if (!hasMapLocation.value) return
  try {
    await loadKakaoMap()
  } catch (error) {
    console.error('Kakao map load failed', error)
    mapError.value = true
    return
  }
  await nextTick()
  if (!kakaoMapRef.value || !window.kakao?.maps?.load) return
  window.kakao.maps.load(async () => {
    const coords = await resolveMapCenter()
    if (!coords) {
      mapError.value = true
      return
    }

    const center = new window.kakao.maps.LatLng(coords.latitude, coords.longitude)
    const needsNewMap = !kakaoMap
      || (typeof kakaoMap.getContainer === 'function' && kakaoMap.getContainer() !== kakaoMapRef.value)

    if (needsNewMap) {
      kakaoMap = new window.kakao.maps.Map(kakaoMapRef.value, {
        center,
        level: 3
      })
      kakaoMarker = new window.kakao.maps.Marker({ position: center })
      kakaoMarker.setMap(kakaoMap)
    } else {
      kakaoMap.setCenter(center)
      kakaoMarker?.setPosition(center)
    }
  })
}

onMounted(() => {
  renderKakaoMap()
})

watch(
  () => [props.latitude, props.longitude, mapAddress.value],
  () => {
    resolvedAddressCoords.value = null
    renderKakaoMap()
  }
)
</script>

<template>
  <section class="section map-section">
    <h2>숙소 위치</h2>
    <div v-if="hasMapLocation && !mapError" ref="kakaoMapRef" class="map-container"></div>
    <div v-else class="map-placeholder">
      <div class="map-text">위치 정보가 없습니다.</div>
    </div>
    <p class="map-info">{{ transportText }}</p>
  </section>
</template>

<style scoped>
.section {
  padding: 1.5rem 0;
}
h2 {
  font-size: 1.4rem;
  margin-bottom: 1rem;
}
.map-placeholder {
  background: #eee;
  width: 100%;
  max-width: 520px;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  color: #888;
  padding: 0 1rem;
  text-align: center;
  margin: 0 auto;
}
.map-text {
  color: var(--text-main);
  font-weight: 600;
}
.map-container {
  width: 100%;
  max-width: 520px;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  margin: 0 auto;
}
.map-info {
  margin-top: 0.5rem;
}
@media (min-width: 769px) {
  .map-placeholder,
  .map-container {
    max-width: 100%;
    aspect-ratio: auto;
    height: 480px;
  }
}
</style>
