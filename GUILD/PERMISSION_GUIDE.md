# PermissionLevel システム - 実装ガイド

## 概要

`PermissionLevel` は、コマンドの権限管理を**簡単かつ直感的**に行うための列挙型です。
5段階の権限レベルで、ほとんどのユースケースをカバーできます。

## 権限レベルの種類

### 0: ANY - 誰でも実行可能

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ANY;
}
```

**実行可能なユーザー:**
- ✅ 通常プレイヤー
- ✅ Op プレイヤー
- ✅ コンソール

**使用例:**
- 情報表示コマンド
- ステータスコマンド
- ヘルプコマンド

---

### 1: MEMBER - メンバー以上

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.MEMBER;
}
```

**実行可能なユーザー:**
- ✅ 通常プレイヤー
- ✅ Op プレイヤー
- ✅ コンソール

**使用例:**
- プレイヤー情報表示
- スコアボード更新
- ゲーム統計情報表示

---

### 2: ADMIN - 管理者のみ

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;
}
```

**実行可能なユーザー:**
- ❌ 通常プレイヤー（実行不可）
- ✅ Op プレイヤー のみ（実装は sender.isOp() に依存）
- ❌ コンソール（実行不可）

**使用例:**
- ゲーム設定変更
- ゲームモード切り替え
- プレイヤーデータ削除

---

### 3: CONSOLE - コンソール専用

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.CONSOLE;
}
```

**実行可能なユーザー:**
- ❌ 通常プレイヤー（実行不可）
- ❌ Op プレイヤー（実行不可）
- ✅ コンソール のみ

**使用例:**
- サーバーシャットダウン
- バックアップ
- メンテナンス処理
- ログ出力

---

### 4: ADMIN_OR_CONSOLE - 管理者またはコンソール

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN_OR_CONSOLE;
}
```

**実行可能なユーザー:**
- ❌ 通常プレイヤー（実行不可）
- ✅ Op プレイヤー
- ✅ コンソール

**使用例:**
- 危険な設定変更
- システムアップデート
- 大規模ゲーム設定変更

---

## 権限チェックの優先度

1. **カスタム権限** (`getPermission()`) - 最優先
2. **権限レベル** (`getPermissionLevel()`) - 次優先

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;  // フォールバック
}

@Override
public String getPermission() {
    return "pexsurvival.custom.perm";  // これが優先される
}
```

実装上の注記:
- `CommandManager.CommandWrapper` 内では最初に `permLevel.hasAccess(sender, command.getPermission())` を呼んでおり、`getPermission()` が null/空でない場合は `sender.hasPermission(customPermission)` が評価されます。
- つまりカスタム権限ノードを返すコマンドは、OP 判定の ADMIN とは独立して許可が与えられます。

## 実装パターン

### パターン1: シンプルなコマンド

```java
public class InfoCommand extends BaseCommand {
    @Override
    public String getName() { return "info"; }
    
    @Override
    public String getDescription() { return "情報を表示"; }
    
    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ANY;  // デフォルトなので省略可能
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sendInfo(sender, "これは情報です");
        return true;
    }
}
```

### パターン2: 管理者専用

```java
public class ConfigCommand extends BaseCommand {
    @Override
    public String getName() { return "config"; }
    
    @Override
    public String getDescription() { return "設定管理"; }
    
    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;  // Op のみ
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Op のみが到達
        sendSuccess(sender, "設定を更新しました");
        return true;
    }
}
```

### パターン3: コンソール専用

```java
public class UpdateCommand extends BaseCommand {
    @Override
    public String getName() { return "update"; }
    
    @Override
    public String getDescription() { return "プラグイン更新"; }
    
    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.CONSOLE;  // コンソール専用
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // コンソールのみが到達
        sendInfo(sender, "更新を確認中...");
        return true;
    }
}
```

### パターン4: カスタム権限との組み合わせ

```java
public class CustomCommand extends BaseCommand {
    @Override
    public String getName() { return "custom"; }
    
    @Override
    public String getDescription() { return "カスタムコマンド"; }
    
    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;  // Op またはカスタム権限
    }
    
    @Override
    public String getPermission() {
        return "pexsurvival.custom.advanced";  // カスタム権限が優先
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sendSuccess(sender, "カスタム権限を確認しました");
        return true;
    }
}
```

## 権限レベルの判定ロジック

```
┌─ コマンド実行 ───────────────┐
│                              │
│ ① カスタム権限あり？         │
│    YES → hasPermission() チェック
│    NO → ②へ
│                              │
│ ② 権限レベルでチェック       │
│    ANY        → 実行可能      │
│    MEMBER     → プレイヤー以上
│    ADMIN      → Op のみ        │
│    CONSOLE    → コンソール のみ
│    ADMIN_OR_CONSOLE → Op or コンソール
│                              │
└──────────────────────────────┘
```

## よくある間違い

### ❌ 間違い: 権限レベルを指定しない場合

```java
public class MyCommand extends BaseCommand {
    // getPermissionLevel() を実装していない
    // → デフォルトで ANY になる（誰でも実行可能）
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 管理者コマンドなのに誰でも実行可能！
        return true;
    }
}
```

### ✅ 正解: 明示的に設定

```java
public class MyCommand extends BaseCommand {
    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;  // 明示的に指定
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Op のみが実行可能
        return true;
    }
}
```

---

### ❌ 間違い: Op でないプレイヤーが実行可能

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.MEMBER;  // プレイヤーなら実行可能
}
```

### ✅ 正解: Op のみに限定

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;  // Op のみ
}
```

## permission.yml の設定

カスタム権限を使用する場合のみ `permission.yml` が必要です。

```yaml
permissions:
  pexsurvival.custom.advanced:
    description: カスタムコマンドの実行権限
    default: op          # op: Op のみ、true: 全員

  pexsurvival.custom.player:
    description: プレイヤーコマンドの実行権限
    default: true        # 全員実行可能
```

## チェックリスト

- [ ] 権限レベルを明示的に指定している
- [ ] 管理者コマンドは `ADMIN` で指定している
- [ ] コンソール専用は `CONSOLE` で指定している
- [ ] 情報表示系は `ANY` または `MEMBER` で指定している
- [ ] カスタム権限を使う場合は `getPermission()` も実装している
- [ ] テストサーバーで権限を確認している

---

**PermissionLevel システム v2.0**
**Paper 1.21+ 対応**
