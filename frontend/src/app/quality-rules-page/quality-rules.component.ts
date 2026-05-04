import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AfterViewInit } from '@angular/core';
import '@triply/yasgui/build/yasgui.min.css';

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
}

@Component({
  selector: 'app-quality-rules',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './quality-rules.component.html',
  styleUrls: ['./quality-rules.component.scss'],
})
export class QualityRulesComponent implements OnInit, AfterViewInit {
  private readonly API = '/api/qr'


  /* ── State ── */
  searchQuery   = signal('');
  activeFilter  = signal('ALL');
  isUploading   = false;

  showEditModal     = false;
  showContentModal  = false;
  showValidateModal = signal(false);
  showAboutModal = false;

  editingRule:     QualityRule | null = null;
  contentViewRule: QualityRule | null = null;

  validateEndpoint = '';
  validatePayload  = '';
  isValidating     = false;
  validateResult:  { status: 'success' | 'error'; message: string } | null = null;

  yasgui: any = null;

  toast = { show: false, message: '', type: 'info' };

  /* ── Enum values exposed to template ── */
  ruleTypes = Object.values(RuleType);

  /* ── Data ── */
  rules = signal<QualityRule[]>([]);

  /* ── Computed ── */
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

  constructor(private http: HttpClient) {}
  ngOnInit(): void {
    this.loadRules();
  }
  ngAfterViewInit(): void {}

  trackById(_: number, rule: QualityRule): string { return rule.id; }


  /* ── Edit / New ── */
  openNewRule(): void {
    this.editingRule = {
      id:'', name: '', description: '', content: '',
      ruleType: RuleType.SPARQL
    };
    this.showEditModal = true;
    setTimeout(() => this.initYasgui(), 0);
  }

  createRule(rule: QualityRule) {
    return this.http.post<void>(this.API, this.mapToDto(rule));
  }
  updateRule(rule: QualityRule) {
    return this.http.put<void>(`${this.API}/${rule.id}`, this.mapToDto(rule));
  }

  editRule(rule: QualityRule): void {
    this.editingRule = { ...rule };
    this.showEditModal = true;
    setTimeout(() => this.initYasgui(), 0);
  }
  async initYasgui(): Promise<void> {
    if (!this.editingRule || this.editingRule.ruleType !== RuleType.SPARQL) return;
    const container = document.getElementById('yasgui');
    if (!container) return;
    // 🔥 destruir completamente
    if (this.yasgui) {
      this.yasgui.destroy?.(); // por si existe
      this.yasgui = null;
      container.innerHTML = '';
    }
    const Yasgui = (await import('@triply/yasgui')).default;
    this.yasgui = new Yasgui(container, {
      requestConfig: {
        endpoint: this.validateEndpoint || 'http://dbpedia.org/sparql'
      },
      copyEndpointOnNewTab: false,
      persistencyExpire: 0 // 🔥 importante: evita que recupere queries anteriores
    });
    const tab = this.yasgui.getTab();
    // 🔥 FORZAR contenido limpio
    tab.setQuery(this.editingRule.content || '');
  }
  onRuleTypeChange(): void {
    // Esperar a que Angular renderice el nuevo DOM
    setTimeout(() => {
      this.initYasgui();
    }, 0);
  }

  saveRule(): void {
    if (!this.editingRule) return;
    // 🔥 añadir esto
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
        this.showToast('Regla creada', 'success');
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
        this.loadRules()
      },
      error: (err) => {
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }

  closeModal(): void {
    this.showEditModal = false;
    this.editingRule = null;
    // limpiar yasgui
    if (this.yasgui) {
      const container = document.getElementById('yasgui');
      if (container) container.innerHTML = '';
      this.yasgui = null;
    }
  }

  /* ── Content view ── */
  viewContent(rule: QualityRule): void { this.contentViewRule = rule; this.showContentModal = true; }
  closeContentModal(): void { this.showContentModal = false; this.contentViewRule = null; }

  /* ── CSV  ── */
  onCsvUpload(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', file);

    this.http.post<void>(`${this.API}/upload`, formData)
      .subscribe({
        next: () => {
          this.showToast('CSV subido correctamente', 'success');
          this.loadRules(); // refrescar tabla
          this.isUploading = false;
          (event.target as HTMLInputElement).value = '';
        },
        error: (err) => {
          this.showToast(this.getErrorMessage(err), 'error');
          this.isUploading = false;
        }
      });
  }

  exportCsv(): void {
    this.http.get(`${this.API}/export`, {
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (res) => {
        const blob = res.body!;

        // Obtener nombre del archivo
        let filename = 'quality_rules.csv';
        const contentDisposition = res.headers.get('Content-Disposition');

        if (contentDisposition) {
          const match = contentDisposition.match(/filename="(.+)"/);
          if (match) filename = match[1];
        }

        // Descargar
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = filename;
        link.click();

        this.showToast('CSV descargado correctamente', 'success');
      },
      error: (err) => {
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }
  /* ── Validate ── */
  openValidateModal(): void {this.showValidateModal.set(true); this.validateResult = null; }
  closeValidateModal(): void { this.showValidateModal.set(false); this.validateResult = null; }

  validateGraph(tipo: 'pdf' | 'csv'): void {
    if (!this.validateEndpoint.trim()) {
      this.showToast('Introduce un endpoint válido', 'error');
      return;
    }
    this.isValidating = true;
    const url = `${this.API}/validate?url=${encodeURIComponent(this.validateEndpoint)}&tipo=${tipo}`;
    this.http.get(url, { responseType: 'blob', observe: 'response' })
      .subscribe({
        next: (res) => {
          this.closeValidateModal();   // 👈 ahora sí Angular lo detecta
          document.body.offsetHeight;
          const blob = res.body!;
          // Obtener nombre del fichero desde headers
          let filename = 'validation_report';
          const contentDisposition = res.headers.get('Content-Disposition');

          if (contentDisposition) {
            const match = contentDisposition.match(/filename="(.+)"/);
            if (match) filename = match[1];
          } else {
            filename += tipo === 'pdf' ? '.pdf' : '.csv';
          }
          // Descargar archivo
          const link = document.createElement('a');
          link.href = window.URL.createObjectURL(blob);
          link.download = filename;
          link.click();
          this.validateResult = {
            status: 'success',
            message: 'Validación completada y descargada'
          };
          this.isValidating = false;
        },
        error: (err) => {
          this.validateResult = {
            status: 'error',
            message: this.getErrorMessage(err)
          };
          this.isValidating = false;
        }
      });
  }

  /* ── Toast ── */
  showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toast = { show: true, message, type };
    setTimeout(() => { this.toast = { ...this.toast, show: false }; }, 3000);
  }

  openAbout(): void {
    this.showAboutModal = true;
  }

  closeAboutModal(): void {
    this.showAboutModal = false;
  }

  private mapToDto(rule: QualityRule) {
    return {
      content: rule.content,
      type: rule.ruleType,
      name: rule.name,
      description: rule.description
    };
  }
  private loadRules(): void {

    this.http.get<QualityRule[]>(this.API)
      .subscribe({
        next: (data) => {
          this.rules.set(data);
        },
        error: (err) => {
          console.error('Error cargando reglas', err);
          this.showToast(this.getErrorMessage(err), 'error');
        }
      });
  }
  private getErrorMessage(err: any): string {
    if (!err) return 'Error desconocido';
    // Caso 1: backend devuelve texto plano
    if (typeof err.error === 'string') {
      return err.error;
    }
    // Caso 2: JSON con message
    if (err.error?.message) {
      return err.error.message;
    }
    // Caso 3: HttpErrorResponse estándar
    if (err.status) {
      return `Error ${err.status}: ${err.statusText || 'Error en servidor'}`;
    }
    return 'Error inesperado';
  }
}
