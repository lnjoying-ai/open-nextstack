<template>
  <div class="drawerPage">
    <!-- echart -->
    <div :id="id" :style="{ height: height, width: width }" style="min-height: 150px" />
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

const chart: any = ref(null);
const total = ref(0);
const initChart = () => {
  chart.value.setOption({
    title: {
      text: proxy.$t('dashboard.hostCount'),
      // curData内value的总和,
      subtext: curData.value.reduce((a: any, b: any) => a + b.value, 0),
      top: '35%',
      left: 'center',
      textStyle: {
        color: '#000',
        fontWeight: 'normal',
        fontSize: 16,
      },
      subtextStyle: {
        color: '#000',
        fontWeight: 'normal',
        fontSize: 20,
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
              fontSize: 10,
              color: '#999',
            },
          },
        },
        labelLine: {
          length: 15,
          length2: 0,
          maxSurfaceAngle: 80,
        },
        labelLayout(params: any) {
          const isLeft = params.labelRect.x < chart.value.getWidth() / 2;
          const points = params.labelLinePoints;
          // Update the end point.
          points[2][0] = isLeft ? params.labelRect.x : params.labelRect.x + params.labelRect.width;
          return {
            labelLinePoints: points,
          };
        },
        data: curData.value,
      },
    ],
  });
};
onMounted(() => {
  const dom: any = document.getElementById(id.value);
  chart.value = echarts.init(dom);
  initChart();
  window.addEventListener('resize', () => {
    chart.value.resize();
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
