package org.pexserver.koukunn.pexsurvival.Core.Feature;

import org.bukkit.event.Listener;

/**
 * プラグイン機能の基底インターフェース
 * すべての機能はこのインターフェースを実装し、イベントリスナーとして機能します
 */
public interface Feature extends Listener {

    /**
     * 機能の名前を返します
     * @return 機能名（/pex toggle <name> で使用）
     */
    String getFeatureName();

    /**
     * 機能の説明を返します
     * @return 機能の説明
     */
    String getDescription();

    /**
     * 機能が有効かどうかを返します
     * @return 有効な場合 true
     */
    boolean isEnabled();

    /**
     * 機能を有効にします
     */
    void enable();

    /**
     * 機能を無効にします
     */
    void disable();

    /**
     * 機能をリロードします
     */
    void reload();
}
