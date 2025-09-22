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
  const data1: any = [];
  let data2: any = 0;
  curData.value.forEach((item: any) => {
    data1.push({ value: item.value, name: item.name });
    data2 += item.value;
  });

  chart.setOption({
    title: [
      {
        // 标题
        text: proxy.$t('devOps.alert.weekDistribution'),
        textStyle: {
          fontSize: 14,
          fontWeight: 'normal',
          color: '#333',
        },
        left: '20px',
      },
      {
        subtext: data2,
        subtextStyle: {
          fontSize: 24,
          fontWeight: 'normal',
          color: '#333',
        },
        left: '29.5%',
        top: '40%',
        textAlign: 'center',
      },
    ],
    tooltip: {
      trigger: 'item',
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: '15%',
      top: 'center',
    },
    series: [
      {
        name: proxy.$t('devOps.alert.weekDistribution'),
        type: 'pie',
        radius: ['40%', '60%'],
        center: ['30%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 1,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {},
        labelLine: {
          show: false,
        },
        data: data1,
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
