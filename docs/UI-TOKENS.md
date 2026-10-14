# 农家乐旅游网站 — UI 设计令牌（视觉）

> 维护：资深UI/UE设计师 · 色板 / 字体 / 间距 / 圆角 / 断点 / 阴影 / 动效  
> 状态：一期**中性占位**；品牌名与主色老板确认后替换本节并同步前端 CSS 变量  
> 对齐：DESIGN.md（交互）· PRODUCT.md（范围）· 前端实现跟项目组件库  
> 更新：2026-10-14（上海）

## 1. 原则

- 已有组件库能完成的，**沿用组件**，不另造视觉语言。
- 令牌用 CSS 变量命名；门户轻量布局，管理端跟随 Element Plus（或项目已有库）语义色映射到下表。
- 正文与背景对比：**正文 ≥ 4.5:1**，大号标题 / 图标旁文字 ≥ 3:1。

## 2. 颜色

| Token | 用途 | 占位值 | 备注 |
|-------|------|--------|------|
| `--color-brand` | 主品牌 / 主按钮 | `#2F6B4F` | 乡村绿占位，可替换 |
| `--color-brand-hover` | 主按钮悬停 | `#265A42` | |
| `--color-brand-subtle` | 浅底强调 | `#E8F2EC` | 标签、选中条 |
| `--color-text` | 主文案 | `#1F2937` | |
| `--color-text-secondary` | 次要说明 | `#6B7280` | 起价旁备注、时间线 |
| `--color-text-inverse` | 深色底上文字 | `#FFFFFF` | |
| `--color-border` | 分割线 / 卡片边 | `#E5E7EB` | |
| `--color-bg` | 页面底 | `#F7F8FA` | |
| `--color-surface` | 卡片 / 表单底 | `#FFFFFF` | |
| `--color-success` | 成功 | `#15803D` | 成功页提示 |
| `--color-warning` | 警告 | `#B45309` | 二次确认 |
| `--color-danger` | 错误 / 破坏 | `#B91C1C` | 校验失败、下架 |
| `--color-link` | 链接 / 电话 | `#1D4ED8` | `tel:` 可点样式 |
| `--color-disabled` | 禁用 | `#9CA3AF` | 下架 CTA |

语义映射（管理端 Element Plus 示例）：`primary` → brand；`success` / `warning` / `danger` 同上。

## 3. 字体

| Token | 用途 | 规格 |
|-------|------|------|
| `--font-family` | 全局 | 系统栈：`"PingFang SC", "Microsoft YaHei", system-ui, sans-serif` |
| `--font-size-xs` | 辅助、时间戳 | 12px / 18px 行高 |
| `--font-size-sm` | 次要正文、表单提示 | 14px / 22px |
| `--font-size-md` | 正文、列表 | 16px / 24px |
| `--font-size-lg` | 卡片标题、区块标题 | 18px / 28px |
| `--font-size-xl` | 页面标题 | 24px / 32px |
| `--font-weight-regular` | 正文 | 400 |
| `--font-weight-medium` | 强调、按钮 | 500 |
| `--font-weight-semibold` | 页面标题 | 600 |

价格「起」用 `--font-size-lg` + medium；整数价可略大一档，小数/「起」用 secondary 色。

## 4. 间距与栅格

| Token | 值 | 用途 |
|-------|-----|------|
| `--space-1` | 4px | 紧凑图标间距 |
| `--space-2` | 8px | 标签内边距 |
| `--space-3` | 12px | 表单项内 |
| `--space-4` | 16px | 卡片内边距（窄屏） |
| `--space-5` | 24px | 区块间距、卡片内（宽屏） |
| `--space-6` | 32px | 页面左右边距 |
| `--space-7` | 48px | 大区块分隔 |

内容最大宽：门户主栏 **1120px** 居中；管理端跟表格库默认。列表栅格：宽屏 3～4 列卡片，平板 2 列，窄屏 1 列。

## 5. 圆角与阴影

| Token | 值 |
|-------|-----|
| `--radius-sm` | 4px（按钮小、标签） |
| `--radius-md` | 8px（卡片、输入框） |
| `--radius-lg` | 12px（大图集外框） |
| `--shadow-card` | `0 1px 3px rgba(0,0,0,.08)` |
| `--shadow-sticky` | `0 -2px 8px rgba(0,0,0,.06)`（窄屏底栏 CTA） |

## 6. 断点

| 名称 | 宽度 | 行为 |
|------|------|------|
| `sm` | < 768px | 单列；详情三 CTA **吸底**；管理端侧栏收起 |
| `md` | 768–1023px | 双列列表 |
| `lg` | ≥ 1024px | 多列；详情 CTA 可在信息区右侧或底部横排 |

## 7. 动效（克制）

- 按钮 / 链接：`150ms ease` 颜色过渡  
- 页面切换：不做夸张动效；加载用组件库 Spinner / Skeleton  
- 禁用：无点击态缩放

## 8. 图标与图片

- 图标：线性 24×24，描边 1.5–2；**必须有可访问名称**（`aria-label` 或旁注文案）  
- 房源图：卡片封面 **4:3**；详情主图 **16:9**；缺图用 `--color-brand-subtle` 占位 + alt「暂无图片」  
- 头像 / 运营头像：一期可不做，用姓名字即可

## 9. 前端落地约定

```css
:root {
  --color-brand: #2F6B4F;
  /* …其余按上表 */
}
```

品牌色变更只改令牌与文档日期，不改交互文案。变更同步 `DESIGN.md` 索引与项目经理。
