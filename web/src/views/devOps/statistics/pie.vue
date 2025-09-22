<template>
  <div class="drawerPage">
    <!-- echart -->
    <div :id="id" :style="{ height: height, width: width }" style="min-height: 150px" />
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';

const props = defineProps({
  className: {
    type: String,
    default: 'chart',
  },
  unit: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '',
  },
  totalNum: {
    type: Number,
    default: 0,
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

const { width, height, unit, title, totalNum, id, curData } = toRefs(props);

let chart: any;
const total: any = ref(0);
const initChart = () => {
  chart.setOption({
    color: ['#0091FF', '#44D7B6'],
    title: {
      text: title.value,
      // curData内value的总和,
      subtext: totalNum.value + unit.value,
      top: '40%',
      left: 'center',
      textStyle: {
        color: '#000',
        fontWeight: 'normal',
        fontSize: 16,
      },
      subtextStyle: {
        color: '#000',
        fontWeight: 'normal',
        fontSize: 16,
      },
    },
    series: [
      {
        type: 'pie',
        radius: [50, 70],
        top: '15%',
        height: '70%',
        left: 'center',
        width: '100%',
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 1,
        },
        label: {
          alignTo: 'edge',
          formatter: '{name|{b}}\n{time|{c}}',
          minMargin: 5,
          edgeDistance: 10,
          lineHeight: 15,
          rich: {
            time: {
              fontSize: 12,
              color: '#333',
            },
          },
        },
        labelLine: {
          length: 15,
          length2: 0,
          maxSurfaceAngle: 80,
        },

        data: curData.value,
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
