<script setup>
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
} from 'chart.js'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const props = defineProps({
  labels: { type: Array, required: true },
  values: { type: Array, required: true },
  unit: { type: String, default: 'KRW' },
  height: { type: Number, default: 260 },
  dense: { type: Boolean, default: false }
})

const formatValue = (value) => {
  const numeric = Number(value ?? 0)
  if (props.unit === 'COUNT') return `${numeric.toLocaleString()}`
  return `₩${numeric.toLocaleString()}`
}

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      data: props.values,
      backgroundColor: '#e5f3ef',
      hoverBackgroundColor: '#14b8a6',
      borderColor: '#0f766e',
      borderWidth: 1,
      hoverBorderWidth: 2,
      borderRadius: 8,
      barPercentage: 0.7,
      categoryPercentage: 0.8
    }
  ]
}))

const chartOptions = computed(() => {
  const total = props.labels.length
  const last = Math.max(total - 1, 0)
  const maxTicks = 8
  const step = total > 1 ? Math.ceil(last / (maxTicks - 1)) : 1
  const maxValue = props.values.length ? Math.max(...props.values.map((v) => Number(v ?? 0)), 1) : 1
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          title: (items) => items?.[0]?.label ?? '',
          label: (item) => formatValue(item.parsed?.y ?? 0)
        }
      }
    },
    interaction: { mode: 'nearest', intersect: true },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          autoSkip: props.dense,
          maxTicksLimit: props.dense ? maxTicks : total,
          maxRotation: 35,
          minRotation: 35,
          callback: (value, index) => {
            if (!props.dense) return String(props.labels[index] ?? '').slice(5)
            if (index === 0 || index === last || index % step === 0) {
              return String(props.labels[index] ?? '').slice(5)
            }
            return ''
          }
        }
      },
      y: {
        beginAtZero: true,
        suggestedMin: 0,
        suggestedMax: maxValue,
        ticks: {
          callback: (value) => formatValue(value)
        }
      }
    }
  }
})
</script>

<template>
  <div class="admin-bar-chart" :style="{ height: `${height}px` }">
    <Bar :data="chartData" :options="chartOptions" />
  </div>
</template>

<style scoped>
.admin-bar-chart {
  position: relative;
  width: 100%;
}
</style>
