import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AfterViewInit } from '@angular/core';
import { Router } from '@angular/router';
import '@triply/yasgui/build/yasgui.min.css';
import { ViewEncapsulation } from '@angular/core';

export enum RuleType {
  SPARQL = 'SPARQL',
  SHACL  = 'SHACL',
}

export interface QualityRule {
  id: string;
  name: string;
  description: string;
  content: string;
  ruleType: RuleType;
  enabled:boolean
}

@Component({
  selector: 'app-quality-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './quality-rules.component.html',
  styleUrls: ['./quality-rules.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class QualityRulesComponent implements OnInit, AfterViewInit {
  private readonly API = '/api/qr';
  private readonly DATASET_API = '/api/kg';

  searchQuery   = signal('');
  activeFilter  = signal('ALL');



  importDatasetName  = '';
  importEndpointUrl  = '';
  isImporting        = false;

  showEditModal     = false;
  showContentModal  = false;
  showAboutModal    = false;

  editingRule:     QualityRule | null = null;
  contentViewRule: QualityRule | null = null;

  yasgui: any = null;

  toast = signal({ show: false, message: '', type: 'info' as 'success' | 'error' | 'info' });

  ruleTypes = Object.values(RuleType);

  rules = signal<QualityRule[]>([]);

  filteredRules = computed(() =>
    this.rules().filter(r => {
      const matchName   = r.name.toLowerCase().includes(this.searchQuery().toLowerCase());
      const matchFilter = this.activeFilter() === 'ALL' || r.ruleType === this.activeFilter();
      return matchName && matchFilter;
    })
  );

  countByType(type: string): number {
    return this.rules().filter(r => r.ruleType === type).length;
  }

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.loadRules();
  }

  ngAfterViewInit(): void {}

  trackById(_: number, rule: QualityRule): string { return rule.id; }


  importDataset(): void {
    if (!this.importEndpointUrl.trim()) {
      this.showToast('Introduce una URL de endpoint válida', 'error');
      return;
    }
    if (!this.importDatasetName.trim()) {
      this.showToast('Introduce un nombre para el dataset', 'error');
      return;
    }

    this.isImporting = true;

    this.http.post(this.DATASET_API, {
      name: this.importDatasetName.trim(),
      endpointUrl: this.importEndpointUrl.trim(),
    }, {
      responseType: 'text'
    }).subscribe({
      next: (msg) => {
        this.showToast(msg, 'success');
        this.importDatasetName = '';
        this.importEndpointUrl = '';
        this.isImporting = false;
      },
      error: (err) => {
        this.showToast(this.getErrorMessage(err), 'error');
        this.isImporting = false;
      }
    });
  }


  goToDatasets(): void {
    this.router.navigate(['/datasets']);
  }


  openNewRule(): void {
    this.editingRule = {
      id: '', name: '', description: '', content: '',
      ruleType: RuleType.SPARQL,
      enabled:true,
    };
    this.showEditModal = true;
    setTimeout(() => this.initYasgui(), 0);
  }

  createRule(rule: QualityRule) {
    return this.http.post(this.API, this.mapToDto(rule), { responseType: 'text' });
  }

  updateRule(rule: QualityRule) {
    return this.http.put(`${this.API}/${rule.id}`, this.mapToDto(rule), { responseType: 'text' });
  }

  editRule(rule: QualityRule): void {
    this.editingRule = { ...rule };
    this.showEditModal = true;
    setTimeout(() => this.initYasgui(), 0);
  }

  async initYasgui(): Promise<void> {
    try {
      if (!this.editingRule || this.editingRule.ruleType !== RuleType.SPARQL) return;

      const container = document.getElementById('yasgui');
      if (!container) return;

      if (this.yasgui) {
        this.yasgui.destroy?.();
        this.yasgui = null;
        container.innerHTML = '';
      }

      const Yasgui = (await import('@triply/yasgui')).default;

      this.yasgui = new Yasgui(container, {
        copyEndpointOnNewTab: false,
        persistencyExpire: 0,
        populateFromUrl: false,
        autofocus: false,
        requestConfig: { endpoint: '' },
        yasqe: { showQueryButton: false },
      });

      const tab = this.yasgui.getTab();
      tab.setQuery(this.editingRule.content || '');

    } catch (error) {
      console.error('Error inicializando YASGUI', error);
    }
  }

  onRuleTypeChange(): void {
    setTimeout(() => this.initYasgui(), 0);
  }

  saveRule(): void {
    if (!this.editingRule) return;
    if (this.editingRule.ruleType === RuleType.SPARQL && this.yasgui) {
      this.editingRule.content = this.yasgui.getTab().getQuery();
    }
    if (!this.editingRule.name.trim()) { this.showToast('El nombre es requerido', 'error'); return; }
    if ((this.editingRule.content?.length ?? 0) > 5000) { this.showToast('Contenido excede 5000 caracteres', 'error'); return; }

    const isEdit = !!this.editingRule.id;
    const request = isEdit
      ? this.updateRule(this.editingRule)
      : this.createRule(this.editingRule);

    request.subscribe({
      next: () => {
        this.showToast(isEdit ? 'Regla actualizada' : 'Regla creada', 'success');
        this.loadRules();
        this.closeModal();
      },
      error: (err) => {
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }

  deleteRuleApi(id: string) {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  deleteRule(id: string): void {
    this.deleteRuleApi(id).subscribe({
      next: () => {
        this.rules.update(rs => rs.filter(r => r.id !== id));
        this.showToast('Regla eliminada', 'success');
        this.loadRules();
      },
      error: (err) => {
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }

  closeModal(): void {
    this.showEditModal = false;
    this.editingRule = null;
    if (this.yasgui) {
      const container = document.getElementById('yasgui');
      if (container) container.innerHTML = '';
      this.yasgui = null;
    }
  }

  viewContent(rule: QualityRule): void { this.contentViewRule = rule; this.showContentModal = true; }
  closeContentModal(): void { this.showContentModal = false; this.contentViewRule = null; }


  openAbout(): void { this.showAboutModal = true; }
  closeAboutModal(): void { this.showAboutModal = false; }

  showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toast.set({ show: true, message, type });
    setTimeout(() => {
      this.toast.update(t => ({ ...t, show: false }));
    }, 3000);
  }

  private mapToDto(rule: QualityRule) {
    return {
      content:     rule.content,
      type:        rule.ruleType,
      name:        rule.name,
      description: rule.description,
    };
  }

  private loadRules(): void {
    this.http.get<QualityRule[]>(this.API).subscribe({
      next:  (data) => this.rules.set(data),
      error: (err)  => {
        console.error('Error cargando reglas', err);
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }

  private getErrorMessage(err: any): string {
    if (!err) return 'Error desconocido';
    if (typeof err.error === 'string') return err.error;
    if (err.error?.message) return err.error.message;
    if (err.status) return `Error ${err.status}: ${err.statusText || 'Error en servidor'}`;
    return 'Error inesperado';
  }
}
