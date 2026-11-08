import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { DynamicCategory } from '../types';
import { Plus, Edit2, Trash2, Check, X, RefreshCw, Layers, Sparkles, AlertCircle } from 'lucide-react';

interface CategoriesManagementProps {
  canManage: boolean;
  actorName: string;
}

export const CategoriesManagement: React.FC<CategoriesManagementProps> = ({ canManage, actorName }) => {
  const [categories, setCategories] = useState<DynamicCategory[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [editingCategory, setEditingCategory] = useState<DynamicCategory | null>(null);

  // Form State
  const [formNameSo, setFormNameSo] = useState<string>('');
  const [formNameEn, setFormNameEn] = useState<string>('');
  const [formEmoji, setFormEmoji] = useState<string>('✨');
  const [formSubcategoriesRaw, setFormSubcategoriesRaw] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('dynamic_business_categories')
        .select('*')
        .order('sort_order', { ascending: true });

      if (error) {
        console.warn('Could not fetch categories from Supabase, error:', error.message);
      } else if (data && data.length > 0) {
        setCategories(data as DynamicCategory[]);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOpenAdd = () => {
    setEditingCategory(null);
    setFormNameSo('');
    setFormNameEn('');
    setFormEmoji('✨');
    setFormSubcategoriesRaw('');
    setShowAddModal(true);
  };

  const handleOpenEdit = (cat: DynamicCategory) => {
    setEditingCategory(cat);
    setFormNameSo(cat.name_so);
    setFormNameEn(cat.name_en);
    setFormEmoji(cat.emoji);
    setFormSubcategoriesRaw(cat.subcategories.map(s => s.nameSo).join(', '));
    setShowAddModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canManage) {
      alert('Ma lihid awoodda aad wax kaga beddesho qeybaha.');
      return;
    }

    if (!formNameSo.trim()) {
      alert('Fadlan gali magaca Soomaaliga ee qeybta');
      return;
    }

    setIsSubmitting(true);
    try {
      const subList = formSubcategoriesRaw.split(',')
        .map(s => s.trim())
        .filter(s => s.length > 0)
        .map(subName => ({
          id: subName.toLowerCase().replace(/\s+/g, '_'),
          nameSo: subName,
          nameEn: subName,
          emoji: formEmoji
        }));

      const catId = editingCategory ? editingCategory.id : `cat_${Date.now()}`;
      const payload = {
        id: catId,
        name_so: formNameSo.trim(),
        name_en: formNameEn.trim() || formNameSo.trim(),
        emoji: formEmoji.trim() || '✨',
        banner_image_url: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=80',
        is_active: true,
        sort_order: editingCategory ? editingCategory.sort_order : categories.length + 1,
        subcategories: subList,
        updated_at: new Date().toISOString()
      };

      const { error } = await supabase
        .from('dynamic_business_categories')
        .upsert(payload, { onConflict: 'id' });

      if (error) {
        throw error;
      }

      setShowAddModal(false);
      fetchCategories();
    } catch (err: any) {
      alert(`Khalad: ${err.message || 'Lama kaydin karin qeybta'}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string, nameSo: string) => {
    if (!canManage) return;
    if (!confirm(`Ma hubtaa inaad rabto inaad tirtirto qeybta '${nameSo}'?`)) return;

    try {
      const { error } = await supabase
        .from('dynamic_business_categories')
        .delete()
        .eq('id', id);

      if (error) throw error;
      fetchCategories();
    } catch (err: any) {
      alert(`Khalad: ${err.message}`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-neutral-900 flex items-center gap-2">
            <Layers className="w-5 h-5 text-red-600" />
            Maamulka Qeybaha Ganacsiga (Categories & Subcategories)
          </h2>
          <p className="text-xs text-neutral-500 mt-1">
            Kudar ama ka saar qeybaha ganacsatadu doortaan marka ay alaabta gelinayaan ama macaamiishu wax ka dhex baaraan.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchCategories}
            className="px-3.5 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-bold rounded-xl transition flex items-center gap-1.5 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
          {canManage && (
            <button
              onClick={handleOpenAdd}
              className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-black rounded-xl shadow-sm transition flex items-center gap-1.5 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              + Kudar Qeyb Cusub
            </button>
          )}
        </div>
      </div>

      {/* Categories Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {categories.map((cat) => (
          <div
            key={cat.id}
            className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs hover:border-neutral-300 transition space-y-3"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-3xl p-2 bg-neutral-100 rounded-xl">{cat.emoji}</span>
                <div>
                  <h4 className="font-black text-sm text-neutral-900">{cat.name_so}</h4>
                  <p className="text-xs text-neutral-500">{cat.name_en}</p>
                </div>
              </div>
              {canManage && (
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => handleOpenEdit(cat)}
                    className="p-1.5 text-neutral-400 hover:text-neutral-900 hover:bg-neutral-100 rounded-lg transition"
                    title="Wax ka beddel"
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => handleDelete(cat.id, cat.name_so)}
                    className="p-1.5 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                    title="Tirtir"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>

            {/* Subcategories preview */}
            <div className="pt-2 border-t border-neutral-100">
              <div className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider mb-1.5">
                Qeybaha Hoose ({cat.subcategories?.length || 0}):
              </div>
              <div className="flex flex-wrap gap-1.5">
                {cat.subcategories && cat.subcategories.length > 0 ? (
                  cat.subcategories.map((sub, idx) => (
                    <span
                      key={idx}
                      className="px-2 py-0.5 bg-neutral-100 text-neutral-700 text-[11px] font-medium rounded-md"
                    >
                      {sub.emoji} {sub.nameSo}
                    </span>
                  ))
                ) : (
                  <span className="text-xs text-neutral-400 italic">Qeybo hoose ma laha</span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Add / Edit Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-4 animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-100">
              <h3 className="text-base font-black text-neutral-900">
                {editingCategory ? 'Wax ka beddel Qeybta' : 'Kudar Qeyb Cusub'}
              </h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-neutral-400 hover:text-neutral-600 p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-neutral-700 mb-1">
                  Magaca Soomaaliga *
                </label>
                <input
                  type="text"
                  value={formNameSo}
                  onChange={(e) => setFormNameSo(e.target.value)}
                  placeholder="Tusaale: Cunto & Maqaayado"
                  className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>

              <div>
                <label className="block font-bold text-neutral-700 mb-1">
                  Magaca English-ka
                </label>
                <input
                  type="text"
                  value={formNameEn}
                  onChange={(e) => setFormNameEn(e.target.value)}
                  placeholder="e.g. Food & Restaurants"
                  className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-bold text-neutral-700 mb-1">
                  Astaanta / Emoji
                </label>
                <input
                  type="text"
                  value={formEmoji}
                  onChange={(e) => setFormEmoji(e.target.value)}
                  placeholder="🍕, 💊, 🛒..."
                  className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div>
                <label className="block font-bold text-neutral-700 mb-1">
                  Qeybaha Hoose (Subcategories - kala sooc faasilo)
                </label>
                <textarea
                  rows={3}
                  value={formSubcategoriesRaw}
                  onChange={(e) => setFormSubcategoriesRaw(e.target.value)}
                  placeholder="Tusaale: Pizza, Burgers, Cunto Dhaqameed, Rooti"
                  className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl font-medium text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                />
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 font-bold rounded-xl transition cursor-pointer"
                >
                  Ka Noqo
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow transition cursor-pointer disabled:opacity-50"
                >
                  {isSubmitting ? 'Kaydinaya...' : 'Kaydi Qeybta'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

