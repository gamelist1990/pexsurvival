# PermissionLevel システム - 実装サマリー

## 🎯 何が変わったか

### 旧システム（String ベース）
```java
@Override
public String getPermission() {
    return "pexsurvival.mycommand";
}
```
- 権限キーを文字列で指定
- 管理者判定が複雑
- 権限設定が必須

### 新システム（PermissionLevel ベース）
```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;
}
```
- 5段階の権限レベルで管理
- 直感的で分かりやすい
- デフォルトで動作（設定不要）
- 既存のカスタム権限にも対応

---

## 📊 権限レベル比較表

| レベル | 値 | 通常プレイヤー | Op プレイヤー | コンソール |
|--------|-----|:---:|:---:|:---:|
| ANY | 0 | ✅ | ✅ | ✅ |
| MEMBER | 1 | ✅ | ✅ | ✅ |
| ADMIN | 2 | ❌ | ✅ | ❌ |
| CONSOLE | 3 | ❌ | ❌ | ✅ |
| ADMIN_OR_CONSOLE | 4 | ❌ | ✅ | ✅ |

---

## 🚀 クイック実装

### 1分で実装できるテンプレート

```java
public class MyCommand extends BaseCommand {
    @Override public String getName() { return "mycommand"; }
    @Override public String getDescription() { return "説明"; }
    @Override public PermissionLevel getPermissionLevel() { return PermissionLevel.ADMIN; }
    @Override public boolean execute(CommandSender sender, String[] args) {
        sendSuccess(sender, "成功");
        return true;
    }
}
```

---

## 📁 ファイル構成

```
Core/
├── PermissionLevel.java     ← 新規！権限レベル定義
├── BaseCommand.java         ← 改良！getPermissionLevel() 追加
├── CommandManager.java      ← 改良！権限チェック改善
├── HelpCommand.java         ← 改良！権限レベル対応
└── CommandGuide.java        ← 改良！新しいガイド

Module/
├── NoJump/
│   └── NoJumpCommand.java   ← 改良！PermissionLevel.ADMIN 使用
└── Examples/
    ├── AdminSettingsCommand.java       ← 新規！管理者専用例
    └── ServerMaintenanceCommand.java   ← 新規！コンソール専用例
```

---

## 🔍 チェックリスト

### BaseCommand の実装

- [ ] `getName()` を実装
- [ ] `getDescription()` を実装  
- [ ] `execute()` を実装
- [ ] `getPermissionLevel()` を実装（オプション、デフォルト: ANY）
- [ ] `getPermission()` を実装（オプション、カスタム権限のみ）
- [ ] `getUsage()` を実装（オプション）

### コマンド登録

- [ ] `Loader.java` の `registerCommands()` で登録
- [ ] コマンドをテストして権限が正しく機能するか確認

---

## 💡 ベストプラクティス

### ✅ 推奨: 権限レベルで管理

```java
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;  // 明確で分かりやすい
}
```

### ✅ 推奨: 必要なレベルだけ指定

```java
// ANY はデフォルトなので省略可能
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ANY;
}

// より簡潔に（ANY がデフォルト）
// getPermissionLevel() を実装しない
```

### ✅ 推奨: 複数レベルが必要な場合

```java
// コンソール or Op が必要
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN_OR_CONSOLE;
}
```

### ⚠️ 注意: カスタム権限はカスタム権限で

```java
// カスタム権限が必須な場合のみ使用
@Override
public String getPermission() {
    return "pexsurvival.custom.advanced";
}
```

---

## 📚 ドキュメント一覧

| ファイル | 説明 |
|---------|------|
| `COMMAND_GUIDE.md` | コマンドシステムの完全ガイド |
| `PERMISSION_GUIDE.md` | PermissionLevel の詳細ガイド |
| `Core/CommandGuide.java` | コード内の実装ガイド |

---

## 🧪 テスト方法

### 権限レベルが正しく機能しているか確認

```
コンソール: /mycommand
→ 結果を確認

通常プレイヤー: /mycommand
→ 権限エラー表示（ADMIN の場合）

Op プレイヤー: /mycommand
→ 正常に実行

/op <プレイヤー名>
/mycommand
→ 実行成功

実装メモ:
- `CommandManager` の `CommandWrapper.execute()` は `PermissionLevel.hasAccess(sender, command.getPermission())` を用いて権限チェックを行います。`getPermission()` が指定されていればそれが優先され、指定がなければ `PermissionLevel` の判定（例: ADMIN は sender.isOp()）が実行されます。

軽いテスト手順:
1. サーバーを起動
2. コンソールで `/pexhelp` を実行してコマンドが登録されていることを確認
3. 非Op プレイヤーで `/pex toggle nojump` を実行して権限エラーが出ることを確認
4. Op プレイヤーで再度 `/pex toggle nojump` を実行し、機能がトグルされることを確認
```

---

## 🔄 移行ガイド（既存プロジェクト）

### Step 1: PermissionLevel.java を追加

### Step 2: BaseCommand.java を更新
- `getPermissionLevel()` メソッドを追加
- `getPermission()` を非抽象メソッドに変更

### Step 3: 既存コマンドを更新
```java
// Before
@Override
public String getPermission() {
    return "pexsurvival.mycommand";
}

// After
@Override
public PermissionLevel getPermissionLevel() {
    return PermissionLevel.ADMIN;
}
```

### Step 4: CommandManager.java を更新
- 権限チェックロジックを改善

### Step 5: テスト実行

---

## 📞 サポート

問題が発生した場合:

1. コンソールのエラーメッセージを確認
2. `PERMISSION_GUIDE.md` を確認
3. `CommandGuide.java` の実装例を参照
4. テストサーバーで権限設定を確認

---

**最終更新**: 2025年10月22日
**バージョン**: v2.0（PermissionLevel ベース）
**対応環境**: Paper 1.21+
