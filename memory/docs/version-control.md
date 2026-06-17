---
name: version-control
description: 版本管理规范和流程
metadata:
  type: project
---

# 版本管理规范

## 1. 分支策略

### 1.1 分支类型
| 分支类型 | 命名规范 | 用途 | 生命周期 |
|---------|---------|------|---------|
| 主分支 | `main` | 生产环境代码，始终保持稳定 | 永久 |
| 开发分支 | `develop` | 日常开发集成分支 | 永久 |
| 功能分支 | `feature/*` | 新功能开发（如：feature/ai-assistant） | 合并后删除 |
| 修复分支 | `hotfix/*` | 紧急bug修复 | 合并后删除 |
| 发布分支 | `release/*` | 版本发布准备（如：release/v1.0.0） | 合并后删除 |

### 1.2 分支工作流程
```
main (生产) ← release/* (发布准备)
     ↑
develop (开发) ← feature/* (新功能)
     ↑
hotfix/* (紧急修复)
```

## 2. 版本号规范

### 2.1 语义化版本格式
```
主版本号.次版本号.修订号（如：1.0.0）

主版本号：不兼容的API修改
次版本号：向下兼容的功能性新增
修订号：向下兼容的问题修正
```

### 2.2 版本号示例
- v1.0.0：首个正式版本
- v1.1.0：新增维度分析功能
- v1.1.1：修复数据校验bug
- v2.0.0：架构重构，不兼容旧版本

## 3. 提交信息规范

### 3.1 格式
```
<类型>(<范围>): <描述>

[可选正文]

[可选脚注]
```

### 3.2 类型说明
- **feat**: 新功能
- **fix**: 修复bug
- **docs**: 文档更新
- **style**: 代码格式调整（不影响逻辑）
- **refactor**: 重构（非新功能、非bug修复）
- **perf**: 性能优化
- **test**: 测试相关
- **chore**: 构建/工具/辅助功能

### 3.3 示例
```
feat(后端): 新增AI智能助手接口

- 实现自然语言问答功能
- 集成Claude API
- 支持经营简报生成

Closes #123
```

## 4. 版本发布流程

### 4.1 发布步骤
```bash
# 1. 从develop创建release分支
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0

# 2. 在release分支进行测试和修复
# ... 测试通过后 ...

# 3. 合并到main并打标签
git checkout main
git pull origin main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "版本v1.0.0发布说明"

# 4. 推送到远程
git push origin main
git push origin v1.0.0

# 5. 合并回develop
git checkout develop
git merge --no-ff release/v1.0.0
git push origin develop

# 6. 删除release分支
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0
```

### 4.2 紧急修复流程
```bash
# 1. 从main创建hotfix分支
git checkout main
git pull origin main
git checkout -b hotfix/v1.0.1

# 2. 修复bug并提交
git add .
git commit -m "fix(模块): 修复xxx问题"

# 3. 合并到main并打标签
git checkout main
git merge --no-ff hotfix/v1.0.1
git tag -a v1.0.1 -m "修复xxx问题"

# 4. 推送
git push origin main
git push origin v1.0.1

# 5. 合并回develop
git checkout develop
git merge --no-ff hotfix/v1.0.1
git push origin develop

# 6. 删除hotfix分支
git branch -d hotfix/v1.0.1
git push origin --delete hotfix/v1.0.1
```

## 5. 代码审查规范

### 5.1 审查要点
- 代码风格是否符合项目规范
- 是否有潜在的bug或安全问题
- 性能是否优化
- 是否有适当的注释和文档
- 测试是否充分

### 5.2 审查流程
1. 开发者创建Pull Request
2. 指定审查者（至少1人）
3. 审查者提出修改意见
4. 开发者根据意见修改
5. 审查通过后合并

## 6. 标签管理

### 6.1 标签命名
```
格式：v主版本号.次版本号.修订号

示例：
- v1.0.0
- v1.1.0
- v1.1.1
```

### 6.2 标签信息
```bash
# 创建附注标签
git tag -a v1.0.0 -m "版本v1.0.0：首次正式发布"

# 查看标签信息
git show v1.0.0

# 推送标签到远程
git push origin v1.0.0
git push origin --tags
```

## 7. 冲突解决规范

### 7.1 解决原则
- 优先保留最新的修改
- 不确定时与相关开发者沟通
- 解决后进行充分测试

### 7.2 解决步骤
```bash
# 1. 更新分支
git checkout feature/xxx
git pull origin develop

# 2. 解决冲突
# 编辑冲突文件，选择正确的代码

# 3. 提交解决结果
git add .
git commit -m "merge: 解决与develop分支的冲突"

# 4. 推送
git push origin feature/xxx
```

## 8. 常用命令速查

### 8.1 分支操作
```bash
# 查看分支
git branch          # 本地分支
git branch -r       # 远程分支
git branch -a       # 所有分支

# 创建分支
git branch <分支名>
git checkout -b <分支名>  # 创建并切换

# 删除分支
git branch -d <分支名>    # 删除已合并分支
git branch -D <分支名>    # 强制删除
```

### 8.2 提交操作
```bash
# 查看状态
git status
git diff

# 提交
git add .
git commit -m "描述"
git commit -am "描述"  # 跳过add，直接提交已跟踪文件

# 推送
git push origin <分支名>
```

### 8.3 标签操作
```bash
# 查看标签
git tag
git tag -l "v1.*"

# 创建标签
git tag v1.0.0
git tag -a v1.0.0 -m "描述"

# 删除标签
git tag -d v1.0.0
git push origin --delete v1.0.0
```

## 9. 注意事项

1. **提交前检查**：确保代码能正常编译运行
2. **小步提交**：每次提交只做一件事，便于追溯
3. **及时拉取**：开发前先拉取最新代码
4. **分支清理**：合并后及时删除临时分支
5. **标签管理**：重要版本必须打标签
6. **文档更新**：重大变更更新CHANGELOG.md
