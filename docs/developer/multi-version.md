# 多版本管理

当需要为不同的 Minecraft 版本维护 NeoTab 时，推荐使用**版本分支策略**。

## 推荐方案：版本分支策略

### 分支结构

```
NeoTab/
├── main                    # 主分支（项目导航页）
├── 1.21.1-neoforge        # Minecraft 1.21.1 版本（当前）
├── 1.21.2-neoforge        # Minecraft 1.21.2 版本（未来）
└── dev                    # 开发分支（可选）
```

### 优点

- 每个版本独立维护，清晰明了
- 可以针对特定版本修复 bug
- 不同版本可以有不同的功能
- CI/CD 友好

## 实施步骤

### 步骤 1：重命名当前分支

```bash
# 将当前 main 分支重命名为 1.21.1-neoforge
git branch -m main 1.21.1-neoforge
git push origin -u 1.21.1-neoforge
git push origin --delete main
```

### 步骤 2：创建新的 main 分支（导航页）

```bash
git checkout --orphan main
git rm -rf .

# 创建简单的 README
# 添加版本导航表格

git add README.md
git commit -m "docs: 创建主分支导航页"
git push -u origin main
```

### 步骤 3：在 GitHub 设置默认分支

进入仓库 Settings → Default branch → 改为 `main`。

### 步骤 4：创建新版本分支

```bash
git checkout 1.21.1-neoforge
git checkout -b 1.21.2-neoforge

# 修改 gradle.properties 中的版本号
# 修改代码以适配新版本

git add .
git commit -m "feat: 适配 Minecraft 1.21.2"
git push -u origin 1.21.2-neoforge
```

## 日常工作流程

### 修复所有版本的通用 Bug

```bash
# 在 1.21.1 修复
git checkout 1.21.1-neoforge
git add .
git commit -m "fix: 修复内存泄漏问题"
git push

# 记录 commit hash
COMMIT_HASH=$(git log -1 --format="%H")

# 应用到 1.21.2
git checkout 1.21.2-neoforge
git cherry-pick $COMMIT_HASH
git push
```

### 发布新版本

```bash
git checkout 1.21.1-neoforge
./gradlew build

git tag v1.0.0-mc1.21.1
git push origin v1.0.0-mc1.21.1
# 在 GitHub 创建 Release，上传 build/libs/*.jar
```

## 命名规范

**分支命名：**
```
<minecraft-version>-<mod-loader>
示例: 1.21.1-neoforge, 1.21.2-neoforge
```

**版本标签：**
```
v<major>.<minor>.<patch>-mc<minecraft-version>
示例: v1.0.0-mc1.21.1, v1.1.0-mc1.21.2
```

**提交信息：**
```
feat: 添加新功能
fix: 修复 bug
docs: 文档更新
perf: 性能优化
refactor: 代码重构
```

## GitHub Actions 自动构建

```yaml
# .github/workflows/build.yml
name: Build

on:
  push:
    branches:
      - '**-neoforge'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: NeoTab-${{ github.ref_name }}
          path: build/libs/*.jar
```

## 常见问题

**Q: 我应该在哪个分支开发新功能？**

在你想支持的最低版本分支上开发，然后 cherry-pick 到其他版本。

**Q: 如何同步多个版本的通用修复？**

使用 `git cherry-pick <commit-hash>` 命令。

**Q: 可以删除旧版本的分支吗？**

可以，但建议保留至少最近 2-3 个版本。
