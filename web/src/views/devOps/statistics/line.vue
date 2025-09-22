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
const total = ref(0);
const initChart = () => {
  if (!curData.value) {
    return;
  }

  const curDataMain: any = curData.value;

  const xAxisData: any = [];
  const data1: any = [];
  curDataMain.forEach((item: any, index: number) => {
    // xAxisData.push(item.createTime.replace('T', '\n').slice(0, -10));
    // xAxisData.push(item.createTime.replace('T', '').slice(0, -18));
    xAxisData.push(item.createTime);
    data1.push(item.used);
  });

  chart.setOption({
    color: ['#0091FF', '#44D7B6'],
    tooltip: {
      trigger: 'axis',

      formatter(params: any) {
        let res = '';
        res += `${params[0].name}<br/>`;
        params.forEach((item: any, index: number) => {
          res += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:${params[index].color}"></span>`;
          res += `${item.seriesName}: ${item.value}${curDataMain[params[index].dataIndex].unit}<br/>`;
        });
        return res;
      },
    },
    legend: {
      data: [proxy.$t('devOps.statistics.line.used')],
      top: '0%',
      right: 10,
    },

    grid: {
      top: '15%',
      left: '5%',
      right: '5%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: [
      {
        type: 'category',
        boundaryGap: false,
        data: xAxisData,
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
    ],
    yAxis: [
      {
        type: 'value',
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
      {
        type: 'value',
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
    series: [
      {
        name: proxy.$t('devOps.statistics.line.used'),
        type: 'line',
        showSymbol: false,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(0,145,255,0.3)',
            },
            {
              offset: 1,
              color: 'rgba(0,145,255,0.05)',
            },
          ]),
        },
        emphasis: {
          focus: 'series',
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
