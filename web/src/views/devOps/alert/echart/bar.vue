<template>
  <div class="drawerPage">
    <!-- echart -->
    <div :id="id" :style="{ height: height, width: width }" style="min-height: 150px; pointer-events: auto" />
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';

const { proxy }: any = getCurrentInstance();
const router = useRouter();
const props = defineProps({
  className: {
    type: String,
    default: 'chart',
  },
  id: {
    type: String,
    default: 'chart',
  },
  width: {
    type: String,
    default: '200px',
  },
  height: {
    type: String,
    default: '200px',
  },
  curData: {
    type: Array,
    default: () => [],
  },
});

const { width, height, id, curData } = toRefs(props);

let chart: any;
const total: any = ref(0);
const initChart = () => {
  if (!curData.value) {
    return;
  }
  const xAxisData: any = [];
  const data0: any = [];
  const data1: any = [];
  const data2: any = [];

  curData.value.forEach((item: any) => {
    xAxisData.push(item.date);
    data0.push(item.warning);
    data1.push(item.critical);
    data2.push(item.emergency);
  });

  chart.setOption({
    title: {
      text: proxy.$t('devOps.alert.week'),
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal',
        color: '#333',
      },
    },
    color: ['#909399', '#E6A23C', '#F56C6C'],
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        // Use axis to trigger tooltip
        type: 'shadow', // 'shadow' as default; can also be 'line' or 'shadow'
      },
    },
    legend: {},
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category',
        data: xAxisData,
      },
    ],
    yAxis: [
      {
        type: 'value',
      },
    ],

    series: [
      {
        name: proxy.$t('devOps.alert.warning'),
        type: 'bar',

        label: {
          show: true,
          color: '#fff',

          textBorderColor: '#909399',
          textBorderWidth: 1,
          textBorderDashOffset: 1,
        },
        barWidth: 16,

        data: data0,
      },
      {
        name: proxy.$t('devOps.alert.serious'),
        type: 'bar',

        label: {
          show: true,
          color: '#fff',
          textBorderColor: '#909399',
          textBorderWidth: 1,
          textBorderDashOffset: 1,
        },
        barWidth: 16,

        data: data1,
      },
      {
        name: proxy.$t('devOps.alert.critical'),
        type: 'bar',

        label: {
          show: true,
          color: '#fff',
          textBorderColor: '#909399',
          textBorderWidth: 1,
          textBorderDashOffset: 1,
        },
        barWidth: 16,

        data: data2,
      },
    ],
  });
};
onMounted(() => {
  const dom: any = document.getElementById(id.value);
  chart = echarts.init(dom);
  initChart();
  window.addEventListener('resize', () => {
    chart.resize();
  });
});
watch(curData, () => {
  initChart();
});
</script>

<style lang="scss" scpoed>
.networkPage {
}
</style>
