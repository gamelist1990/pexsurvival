# Disasters 改善計画

TL;DR — 既存の災害処理を堅牢化し、災害のデフォルト持続時間をソース内ハードコードからグローバル設定（30〜60秒）へ移行します。優先実装の新災害は小工数で表現効果が高い「ToxicFog（毒霧）」を追加します。設定はホットリロード不要、起動時に読み込む方式とします。

## 目的
- 災害の持続時間を現在の 2400–4800 ticks (2–4 分) から 30–60 秒 (600–1200 ticks) に変更する。
- 災害処理の堅牢化（プラグイン参照の安全化、タスクの確実なキャンセル、重複コードの整理）。
- 小工数で見栄えの良い新災害（ToxicFog）を追加する。

## ステップ（優先度順）
1. `NaturalDisasterFeature` の変更
   - `MIN_DISASTER_DURATION` / `MAX_DISASTER_DURATION` を削除し、インスタンスフィールド `minDurationTicks` / `maxDurationTicks` を導入。
   - 起動時に `PEXConfig/naturaldisaster.json` を `ConfigManager` から読み込み、秒単位の値を ticks に変換して設定する。

2. 設定ファイルの追加
   - `PEXConfig/naturaldisaster.json` を追加（デフォルト）:
     ```json
     {
       "default_min_seconds": 30,
       "default_max_seconds": 60,
       "per_disaster": {}
     }
     ```
   - ホットリロード不要。設定はプラグイン起動時にのみ読み込む。

3. `MobPanicDisaster` の修正
   - コメントと実装の整合性: 合計 50 体を目的とするなら spawned 上限を 50 に変更。
   - プラグイン参照を `Bukkit.getPluginManager().getPlugin("pexsurvival")` の乱用から、コンストラクタ注入または `Feature` 経由で渡す。
   - 各モブに紐づく再ターゲット用 `BukkitRunnable` を災害停止時に確実にキャンセルする。
   - スポーン制限とクリーニング（`glowing` フラグやカスタムネームの解除処理）を検討。

4. 共通改善点
   - 複数クラスで使われている `playMiningAnimation` 的な重複コードをユーティリティに抽出。
   - `Particle` の互換フォールバック（既に一部で try/catch を使っているが整理する）。
   - `BukkitRunnable` のキャンセルをプラグインの無効化時に残さない仕組み（`Feature#disable` で参照を保持してキャンセル）。

5. 新災害の追加: `ToxicFogDisaster`
   - 効果: 範囲内のプレイヤーに毒（PotionEffect）を短時間付与し、視界を妨げるパーティクルを表示。小〜中規模のプレイヤー妨害系災害。
   - 実装: `Disaster` を実装する新クラス `ToxicFogDisaster.java` を追加し `DisasterRegistry` に登録。毎秒程度の頻度で周囲プレイヤーを検出して効果付与。

6. ドキュメント更新
   - `GUILD/FEATURE_GUIDE.md` に短い説明と `PEXConfig/naturaldisaster.json` のサンプルを追加。

7. テストと確認
   - サーバを起動して、災害時間が 30–60 秒で割り当てられることを確認。
   - `MobPanic` の spawn 上限やタスクキャンセル、`ToxicFog` の効果が正常かを確認。

## 変更方針・制約
- デフォルトはグローバル設定（per-disaster の拡張は将来対応可能）。
- 新しい設定は起動時のみ読み込む（ホットリロード不要）。
- 既存の `ConfigManager` を流用して `PEXConfig` に JSON ファイルを保管する。
- 互換性重視で既存の機能呼び出し順序は極力変えず、設定化と堅牢化を優先する。

## 予定タスク（短いチェックリスト）
- [ ] `docs/disasters-plan.md` に計画を保存（完了）
- [ ] `NaturalDisasterFeature` の修正
- [ ] `PEXConfig/naturaldisaster.json` の追加
- [ ] `MobPanicDisaster` の修正
- [ ] `ToxicFogDisaster` の実装と登録
- [ ] ドキュメント更新
- [ ] 動作確認

---

