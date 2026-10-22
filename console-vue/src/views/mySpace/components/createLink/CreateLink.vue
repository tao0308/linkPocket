<template>
  <div>
    <div v-if="createdLink" class="created-link" role="status">
      <span class="success-mark"><el-icon><Check /></el-icon></span>
      <h3>你的连接，已准备好发光</h3>
      <p>复制这条短链接，开始分享。</p>
      <ShortLinkGlow :url="createdLink.fullShortUrl" />
      <div class="created-actions"><el-button type="primary" @click="copyCreatedLink">复制链接</el-button><el-button @click="finish">完成</el-button></div>
    </div>
    <el-form v-else ref="ruleFormRef" :model="formData" :rules="formRule" label-width="80px">
      <el-form-item label="跳转链接" prop="originUrl">
        <el-input
          v-if="isSingle"
          v-model="formData.originUrl"
          placeholder="请输入http://或https://开头的链接或应用跳转链接"
        ></el-input>
        <el-input
          v-else
          :rows="4"
          v-model="formData.originUrl"
          type="textarea"
          placeholder="请输入http://或https://开头的链接或应用跳转链接，一行一个，最多100行"
        />
      </el-form-item>
      <el-form-item label="描述信息" prop="describe">
        <el-input
          maxlength="100"
          show-word-limit
          v-loading="isLoading"
          :rows="4"
          v-model="formData.describe"
          type="textarea"
          placeholder="请输入描述信息"
        />
        <span style="font-size: 12px">{{ '将创建' + describeRows + '条短链' }}</span>
      </el-form-item>

      <el-form-item label="短链分组" prop="gid">
        <el-select v-model="formData.gid" placeholder="请选择">
          <el-option
            v-for="item in groupInfo"
            :key="item.gid"
            :label="item.name"
            :value="item.gid"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="有效期" prop="v">
        <el-radio-group v-model="formData.validDateType">
          <el-radio :label="0">永久</el-radio>
          <el-radio :label="1">自定义</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="formData.validDateType === 1" label="选择时间">
        <el-date-picker
          :disabled-date="disabledDate"
          v-model="formData.validDate"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="datetime"
          placeholder="选择日期"
          :shortcuts="shortcuts"
        />
        <span class="alert">链接失效后将自动跳转到404页面 !</span>
      </el-form-item>
      <el-form-item>
        <div style="width: 100%; display: flex; justify-content: flex-end">
          <el-button
            class="buttons"
            type="primary"
            :disabled="submitDisable"
            @click="onSubmit(ruleFormRef)"
            >确认</el-button
          >
          <el-button class="buttons" @click="cancel">取消</el-button>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref, watch, onBeforeUnmount, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { useStore } from 'vuex'
import ShortLinkGlow from '@/components/ShortLinkGlow.vue'
import { isWebsiteUrl } from '@/utils/websiteUrl.mjs'

const store = useStore()
const createdLink = ref(null)
const copyCreatedLink = async () => {
  const url = createdLink.value.fullShortUrl
  try {
    await navigator.clipboard.writeText(/^https?:\/\//i.test(url) ? url : `http://${url}`)
    ElMessage.success('链接已复制')
  } catch { ElMessage.warning('复制未成功，请选中链接手动复制') }
}
const finish = () => emits('onSubmit', false)
const defaultDomain = store.state.domain ?? ' '
const props = defineProps({
  groupInfo: Array,
  isSingle: Boolean, // 单个创建传true， 批量创建传false,
  defaultGid: String
})
const { proxy } = getCurrentInstance()
const API = proxy.$API
// url的校验规则
const reg = { test: isWebsiteUrl }
// 自定义时间中选择几天
const shortcuts = [
  {
    text: '一天',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24)
      return date
    }
  },
  {
    text: '七天',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24 * 7)
      return date
    }
  },
  {
    text: '三十天',
    value: () => {
      const date = new Date()
      date.setTime(date.getTime() + 3600 * 1000 * 24 * 30)
      return date
    }
  }
]
// eslint-disable-next-line vue/no-dupe-keys
const groupInfo = ref()
const formData = reactive({
  domain: defaultDomain,
  originUrl: null,
  gid: null,
  createdType: 1,
  validDate: null,
  describe: null,
  validDateType: 0
})
const initFormData = () => {
  createdLink.value = null
  formData.domain = defaultDomain
  formData.originUrl = null
  formData.createdType = 1
  formData.validDate = null
  formData.describe = null
  formData.validDateType = 0
}
const maxOriginUrlRows = ref(100) // 最多多少行
// 链接有多少行
const originUrlRows = ref(0)
// 防抖
const fd = (fn, delay) => {
  let timer = null
  return function (url) {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    timer = setTimeout(() => {
      fn(url)
    }, delay)
  }
}
const isLoading = ref(false)
const queryTitle = async (url) => {
  if (reg.test(url)) {
    isLoading.value = true
    try {
      const res = await API.smallLinkPage.queryTitle({ url })
      if (formData.originUrl === url && !formData.describe && res.data.success) formData.describe = res.data.data
    } catch { /* The title is optional; users can always enter a description. */ }
    finally { isLoading.value = false }
  }
}
const getTitle = fd(queryTitle, 1000)
watch(
  () => formData.originUrl,
  (nV) => {
    originUrlRows.value = (nV || '').split(/\r|\r\n|\n/)?.length ?? 0
    // 只有在描述内容为空时才会去查询链接对应的标题
    if (!formData.describe) {
      // 外边包一层防抖
      getTitle(nV)
    }
  }
)
const maxDescribeRows = ref(100) // 最多多少行
// 描述信息有多少行
const describeRows = ref(0)
watch(
  () => formData.describe,
  (nV) => {
    describeRows.value = (nV || '').split(/\r|\r\n|\n/)?.length ?? 0
  }
)

// 将分组数据传给选择分组选项并默认选中第一项
watch(
  () => props.groupInfo,
  (nV) => {
    groupInfo.value = nV || []
    // console.log('默认的gid', props.defaultGid)
    if (props.defaultGid) {
      formData.gid = props.defaultGid
    } else {
      formData.gid = nV?.[0]?.gid
    }
  },
  {
    immediate: true
  }
)
watch(
  () => props.defaultGid,
  () => {
    if (props.defaultGid) {
      formData.gid = props.defaultGid
    } else {
      formData.gid = props.groupInfo?.[0]?.gid
    }
  },
  {
    immediate: true
  }
)

// 校验规则
const formRule = reactive({
  originUrl: [
    { required: true, message: '请输入链接', trigger: 'blur' },
    {
      validator: function (rule, value, callback) {
        // console.log('============', value, value.split('/n'))
        if (value) {
          if (value.split(/\r\n|\r|\n/).some(item => !reg.test(item))) {
            return callback(new Error('请输入有效的 HTTP 或 HTTPS 网站链接'))
          }
        }
        if (originUrlRows.value > maxOriginUrlRows.value) {
          callback(new Error('超过输入' + maxOriginUrlRows.value + '行'))
        } else {
          callback()
          submitDisable.value = false
        }
      },
      trigger: 'blur'
    }
  ],
  gid: [{ required: true, message: '请选择分组', trigger: 'blur' }],
  describe: [
    { required: true, message: '请输入描述信息', trigger: 'blur' },
    {
      validator: function (rule, value, callback) {
        // console.log('============', value, value.split('/n'))
        if (props.isSingle === false && describeRows.value !== originUrlRows.value) {
          callback(new Error('标题数量与链接数量不等'))
        }
        if (describeRows.value > maxDescribeRows.value) {
          callback(new Error('超过输入' + maxDescribeRows.value + '行'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  validDate: [
    { required: false, message: '请输日期', trigger: 'blur' }
    // {
    //   validator: function (rule, value, callback) {
    //     if (describeRows.value > maxDescribeRows.value) {
    //       callback(new Error('超过输入' + maxDescribeRows.value + '行'))
    //     } else {
    //       callback()
    //     }
    //   },
    //   trigger: 'blur'
    // }
  ]
})
// 限制日期选择器选择过去的时间
const disabledDate = (time) => {
  return new Date(time).getTime() < new Date().getTime() //选当前时间之后的时间
}

// 将组件里面的确认和取消点击事件传出去
const emits = defineEmits(['onSubmit', 'cancel'])
// 点击确定按钮后的校验
const ruleFormRef = ref()
const submitDisable = ref(false)
const onSubmit = async (formEl) => {
  if (submitDisable.value || !formEl || !await formEl.validate().catch(() => false)) return
  submitDisable.value = true
  try {
    const { data } = await API.smallLinkPage.addSmallLink(formData)
    if (!data?.success) { ElMessage.error(data?.message || '创建失败，请稍后重试'); return }
    createdLink.value = data.data
    ElMessage.success('创建成功！')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建失败，请检查服务连接后重试')
  } finally { submitDisable.value = false }
}
const cancel = () => {
  emits('cancel', false)
  initFormData()
}
onBeforeUnmount(() => {
  initFormData()
})
defineExpose({
  // 当外部对话框以任何方式关闭时，初始化表单数据
  initFormData
})
</script>

<style lang="less" scoped>
.created-link { text-align: center; padding: 22px 10px 30px; }
.success-mark { display: inline-grid; place-items: center; width: 44px; height: 44px; border-radius: 50%; color: #577b69; background: #e6f6ed; font-size: 23px; }
.created-link h3 { margin: 16px 0 8px; color: #26334d; font-size: 22px; }
.created-link p { margin-bottom: 18px; color: #697990; }
.created-actions { display: flex; justify-content: center; gap: 12px; margin-top: 25px; }
.alert {
  color: rgb(231, 166, 67);
  font-size: 12px;
  width: 90%;
}
</style>
