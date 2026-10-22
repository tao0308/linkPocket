<template>
  <el-form ref="ruleFormRef" :model="formData" :rules="formRule" label-width="80px" class="batch-form">
    <el-form-item label="跳转链接" prop="originUrls">
      <el-input v-model="formData.originUrls" type="textarea" :rows="5"
        placeholder="粘贴网站链接，一行一个，最多 10 条（http:// 或 https://）" />
      <div class="input-hint" :class="{ 'over-limit': originUrls.length > MAX_BATCH_LINKS }">
        <span>{{ originUrls.length > MAX_BATCH_LINKS ? '每批最多 10 条，请删除多余链接' : '输入后自动获取每个网站的标题和图标；标题可在下方修改' }}</span>
        <strong>{{ originUrls.length }} / {{ MAX_BATCH_LINKS }}</strong>
      </div>
    </el-form-item>

    <el-form-item v-if="entries.length" label="网站预览" class="preview-item">
      <div class="preview-list">
        <div v-for="(entry, index) in entries" :key="entry.key" class="preview-row">
          <span class="preview-index">{{ index + 1 }}</span>
          <WebsiteIcon :origin-url="entry.url" :size="32" />
          <div class="preview-detail">
            <el-input v-model="entry.title" maxlength="100" :aria-label="`第 ${index + 1} 条短链标题`"
              :placeholder="entry.loading ? '正在获取标题…' : '请输入网站标题'"
              @input="entry.edited = true" />
            <span class="preview-url" :title="entry.url">{{ entry.url }}</span>
          </div>
          <span class="preview-state" aria-live="polite">{{ entry.loading ? '获取中' : entry.failed ? '可手动修改' : '' }}</span>
        </div>
      </div>
    </el-form-item>

    <el-form-item label="短链分组" prop="gid">
      <el-select v-model="formData.gid" placeholder="请选择分组">
        <el-option v-for="item in groupInfo" :key="item.gid" :label="item.name" :value="item.gid" />
      </el-select>
    </el-form-item>
    <el-form-item label="有效期">
      <el-radio-group v-model="formData.validDateType">
        <el-radio :label="0">永久</el-radio>
        <el-radio :label="1">自定义</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item v-if="formData.validDateType === 1" label="选择时间" prop="validDate">
      <el-date-picker v-model="formData.validDate" :disabled-date="disabledDate"
        value-format="YYYY-MM-DD HH:mm:ss" type="datetime" placeholder="选择日期" :shortcuts="shortcuts" />
      <span class="alert">链接失效后将自动跳转到 404 页面</span>
    </el-form-item>
    <el-form-item>
      <div class="actions">
        <el-button type="primary" :loading="submitting" :disabled="hasPendingTitles || originUrls.length > MAX_BATCH_LINKS"
          @click="onSubmit">创建并下载 Excel</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useStore } from 'vuex'
import WebsiteIcon from '@/components/WebsiteIcon.vue'
import { isWebsiteUrl } from '@/utils/websiteUrl.mjs'

const MAX_BATCH_LINKS = 10
const props = defineProps({ groupInfo: Array, defaultGid: String })
const emits = defineEmits(['onSubmit', 'cancel'])
const API = getCurrentInstance().proxy.$API
const defaultDomain = useStore().state.domain ?? ' '
const ruleFormRef = ref()
const entries = ref([])
const submitting = ref(false)
const formData = reactive({
  domain: defaultDomain, originUrls: '', gid: null, createdType: 1,
  validDate: null, validDateType: 0
})
const originUrls = computed(() => formData.originUrls.split(/\r?\n|\r/).map(url => url.trim()).filter(Boolean))
const hasPendingTitles = computed(() => entries.value.some(entry => entry.loading || (!entry.requested && isWebsiteUrl(entry.url))))
let nextKey = 0
let titleTimer
let titleGeneration = 0

const hostTitle = (url) => {
  try { return new URL(url).hostname.replace(/^www\./i, '').slice(0, 100) }
  catch { return '' }
}

const loadTitles = async (generation) => {
  const pending = entries.value.filter(entry => !entry.requested && isWebsiteUrl(entry.url))
  // A small pool prevents ten simultaneous website requests through the backend.
  const workers = Array.from({ length: Math.min(3, pending.length) }, async () => {
    while (pending.length && generation === titleGeneration) {
      const entry = pending.shift()
      entry.requested = true
      entry.loading = true
      try {
        const response = await API.smallLinkPage.queryTitle({ url: entry.url })
        if (entries.value.includes(entry) && !entry.edited && response.data?.success) {
          entry.title = (response.data.data || hostTitle(entry.url)).trim().slice(0, 100)
        }
      } catch {
        if (entries.value.includes(entry)) entry.failed = true
      } finally {
        entry.loading = false
      }
    }
  })
  await Promise.all(workers)
}

watch(originUrls, (urls) => {
  const previous = new Map()
  for (const entry of entries.value) {
    if (!previous.has(entry.url)) previous.set(entry.url, [])
    previous.get(entry.url).push(entry)
  }
  entries.value = urls.slice(0, MAX_BATCH_LINKS).map(url => previous.get(url)?.shift() || {
    key: ++nextKey, url, title: hostTitle(url), edited: false,
    requested: false, loading: false, failed: false
  })
  clearTimeout(titleTimer)
  const generation = ++titleGeneration
  if (urls.length <= MAX_BATCH_LINKS) titleTimer = setTimeout(() => loadTitles(generation), 550)
})

watch(() => [props.groupInfo, props.defaultGid], () => {
  formData.gid = props.defaultGid || props.groupInfo?.[0]?.gid || null
}, { immediate: true })
watch(() => formData.validDateType, type => { if (type === 0) formData.validDate = null })

const formRule = {
  originUrls: [{ validator: (_rule, _value, done) => {
    if (!originUrls.value.length) return done(new Error('请输入至少 1 条网站链接'))
    if (originUrls.value.length > MAX_BATCH_LINKS) return done(new Error('每批最多创建 10 条短链接'))
    if (originUrls.value.some(url => !isWebsiteUrl(url))) return done(new Error('请输入有效的 HTTP 或 HTTPS 网站链接'))
    done()
  }, trigger: 'blur' }],
  gid: [{ required: true, message: '请选择分组', trigger: 'change' }],
  validDate: [{ validator: (_rule, value, done) => {
    if (formData.validDateType === 1 && (!value || new Date(value).getTime() <= Date.now())) {
      return done(new Error('请选择未来的有效时间'))
    }
    done()
  }, trigger: 'change' }]
}
const disabledDate = time => time.getTime() < Date.now() - 86400000
const shortcuts = [1, 7, 30].map((days, index) => ({
  text: ['一天', '七天', '三十天'][index],
  value: () => new Date(Date.now() + days * 86400000)
}))

const downloadExcel = response => {
  const fileName = response.headers['content-disposition']?.match(/filename\*=utf-8''([^;]+)/i)?.[1]
  const url = URL.createObjectURL(new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  }))
  const link = document.createElement('a')
  link.href = url
  link.download = fileName ? decodeURIComponent(fileName) : '批量创建短链接.xlsx'
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

const onSubmit = async () => {
  if (submitting.value || hasPendingTitles.value || !await ruleFormRef.value?.validate().catch(() => false)) return
  const titles = entries.value.map(entry => entry.title.trim())
  if (titles.some(title => !title)) { ElMessage.warning('请填写每条短链接的标题'); return }
  submitting.value = true
  try {
    const response = await API.smallLinkPage.addLinks({
      ...formData, originUrls: originUrls.value, describes: titles
    })
    const bytes = new Uint8Array(response.data)
    if (bytes[0] !== 0x50 || bytes[1] !== 0x4b) {
      let message = '创建失败，请稍后重试'
      try { message = JSON.parse(new TextDecoder().decode(bytes)).message || message } catch { /* Unexpected response. */ }
      ElMessage.error(message)
      return
    }
    downloadExcel(response)
    ElMessage.success('创建成功，短链列表已下载')
    emits('onSubmit', false)
    initFormData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '创建失败，请检查服务连接后重试')
  } finally { submitting.value = false }
}

const initFormData = () => {
  clearTimeout(titleTimer)
  titleGeneration++
  entries.value = []
  formData.originUrls = ''
  formData.domain = defaultDomain
  formData.createdType = 1
  formData.validDate = null
  formData.validDateType = 0
}
const cancel = () => { emits('cancel', false); initFormData() }
onBeforeUnmount(() => { clearTimeout(titleTimer); titleGeneration++ })
defineExpose({ initFormData })
</script>

<style lang="less" scoped>
.batch-form { max-width: 100%; }
.input-hint { width: 100%; display: flex; justify-content: space-between; gap: 12px; color: #75839b; font-size: 12px; line-height: 24px; }
.input-hint strong { white-space: nowrap; color: #6374a0; }.input-hint.over-limit strong { color: #df5353; }
.preview-list { width: 100%; max-height: 286px; overflow: auto; border: 1px solid #e5eaf3; border-radius: 12px; background: #fbfcff; }
.preview-row { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-bottom: 1px solid #edf0f7; }
.preview-row:last-child { border-bottom: 0; }.preview-index { width: 15px; flex: none; color: #8895ac; font-size: 12px; }
.preview-detail { flex: 1; min-width: 0; }.preview-url { display: block; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; color: #8792a8; font-size: 11px; line-height: 20px; }
.preview-state { width: 68px; flex: none; text-align: right; color: #8a96a9; font-size: 11px; }
.actions { width: 100%; display: flex; justify-content: flex-end; }.alert { display: block; color: #d79b3b; font-size: 12px; }
@media (max-width: 600px) { .preview-state { display: none; }.preview-row { gap: 6px; padding: 8px; } }
</style>
