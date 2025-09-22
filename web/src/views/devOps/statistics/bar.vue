<template>
  <div class="drawerPage">
    <!-- echart -->
    <div :id="id" :style="{ height: height, width: width }" style="min-height: 150px; pointer-events: auto" />
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';

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
  const data1: any = [];

  curData.value.forEach((item: any) => {
    // xAxisData.push(item.createTime.replace('T', '\n').slice(0, -10));
    // xAxisData.push(item.createTime.replace('T', '').slice(0, -18));
    xAxisData.push(item.createTime);
    data1.push(item.total);
  });

  chart.setOption({
    color: ['#0091FF', '#44D7B6'],
    tooltip: {},
    xAxis: {
      data: xAxisData,
      name: '',
      axisLine: {
        lineStyle: {
          color: '#f5f5f5',
        },
      },
      axisLabel: {
        color: '#333',
        // 时间截取
        formatter(value: any) {
          return value.slice(5, -9);
        },
      },
    },
    yAxis: [
      {
        type: 'value',
        minInterval: 1,
        // y轴线颜色
        axisLine: {
          lineStyle: {
            color: '#f5f5f5',
          },
        },
        // y轴字体颜色
        axisLabel: {
          color: '#333',
        },
        // y轴分割线颜色
        splitLine: {
          lineStyle: {
            color: '#f5f5f5',
          },
        },
      },
    ],
    grid: {
      top: '5%',
      left: '5%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    series: [
      {
        name: 'NAT',
        type: 'bar',
        stack: 'one',
        barWidth: '40',
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
