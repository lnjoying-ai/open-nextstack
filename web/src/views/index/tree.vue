<template>
  <div class="drawerPage">
    <!-- echart -->
    <div :id="id" :style="{ height: height, width: width }" style="min-height: 150px" />
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import yun from '@/assets/img/yun.png';

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
  curData.value.forEach((item: any) => {
    item.label = {
      color: '#1890ff',
    };
  });
  const data = {
    name: proxy.$t('platform.name'),
    children: curData.value,
  };
  chart.setOption({
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
    },
    series: [
      {
        type: 'tree',
        data: [data],
        top: '1%',
        left: '20%',
        bottom: '1%',
        right: '30%',
        symbol: 'circle',
        symbolSize: 7,
        // symbol颜色
        itemStyle: {
          normal: {
            color: '#1890ff',
          },
        },
        lineStyle: {
          normal: {
            color: '#BAE7FF',
            width: 2,
          },
        },

        label: {
          position: 'left',
          verticalAlign: 'middle',
          align: 'right',
          fontSize: 16,
          color: '#1890ff',
          formatter(params: any) {
            let res = '';
            if (params.dataIndex == 1) {
              res += `{img1|}\n{text1|${params.name}}`;
            } else {
              res += `{img2|}  {text2|${params.name}}`;
            }

            return res;
          },
          rich: {
            img1: {
              backgroundColor: {
                image: yun,
              },
              height: 40,
              align: 'center',
            },
            text1: {
              fontSize: 20,
            },
            img2: {
              backgroundColor: {
                image: yun,
              },
              height: 30,
            },
            text2: {
              fontSize: 16,
            },
          },
        },
        leaves: {
          label: {
            position: 'right',
            verticalAlign: 'middle',
            align: 'left',
          },
        },
        emphasis: {
          focus: 'descendant',
        },
        expandAndCollapse: true,
        animationDuration: 550,
        animationDurationUpdate: 750,
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
