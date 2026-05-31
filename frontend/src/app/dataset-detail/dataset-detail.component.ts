import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { AfterViewInit } from '@angular/core';
import '@triply/yasgui/build/yasgui.min.css';
import { ViewEncapsulation } from '@angular/core';
import { Dataset } from '../dataset/datasets.component';
import { QualityRule, RuleType } from '../quality-rules-page/quality-rules.component';

@Component({
  selector: 'app-dataset-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dataset-detail.component.html',
  styleUrls: ['../quality-rules-page/quality-rules.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class DatasetDetailComponent implements OnInit, AfterViewInit {
  private readonly QR_API      = '/api/qr';
  private readonly DATASET_API = '/api/kg';

  datasetId!: string;
  dataset: Dataset | null = null;

  searchQuery  = signal('');
  activeFilter = signal('ALL');

  showEditModal    = false;
  showContentModal = false;
  showValidateModal = signal(false);
  showAboutModal   = false;

  editingRule:     QualityRule | null = null;
  contentViewRule: QualityRule | null = null;

  isValidating     = false;
  isUploading   = false;

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
  get enabledCount(): number {
    return this.rules().filter(r => r.enabled).length;
  }

  countByType(type: string): number {
    return this.rules().filter(r => r.ruleType === type).length;
  }

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.datasetId = this.route.snapshot.paramMap.get('id')!;
    this.loadDataset();
    this.loadRules();
  }

  ngAfterViewInit(): void {}

  trackById(_: number, rule: QualityRule): string { return rule.id; }


  goToDatasets(): void { this.router.navigate(['/datasets']); }
  goToRules(): void    { this.router.navigate(['/rules']); }


  private loadDataset(): void {
    this.http.get<Dataset>(`${this.DATASET_API}/${this.datasetId}`).subscribe({
      next:  (d)   => this.dataset = d,
      error: (err) => this.showToast(this.getErrorMessage(err), 'error'),
    });
  }

  private loadRules(): void {
    this.http.get<QualityRule[]>(`${this.QR_API}/${this.datasetId}`).subscribe({
      next:  (data) => this.rules.set(data),
      error: (err)  => {
        console.error('Error cargando reglas del dataset', err);
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }
  toggleRule(rule: QualityRule): void {
    this.http.put<QualityRule>(`${this.QR_API}/${rule.id}/toggle`, {}).subscribe({
      next: (updated) => {
        this.rules.update(rs =>
          rs.map(r => r.id === updated.id ? { ...r, enabled: updated.enabled } : r)
        );
      },
      error: (err) => this.showToast(this.getErrorMessage(err), 'error'),
    });
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
      this.yasgui.getTab().setQuery(this.editingRule.content || '');
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
    const url    = isEdit
      ? `${this.QR_API}/${this.editingRule.id}`
      : `${this.QR_API}`;
    const req = isEdit
      ? this.http.put(url, this.mapToDto(this.editingRule), { responseType: 'text' })
      : this.http.post(url, this.mapToDto(this.editingRule), { responseType: 'text' });

    req.subscribe({
      next: () => {
        this.showToast(isEdit ? 'Regla actualizada' : 'Regla creada', 'success');
        this.loadRules();
        this.closeModal();
      },
      error: (err) => this.showToast(this.getErrorMessage(err), 'error'),
    });
  }

  deleteRule(id: string): void {
    this.http.delete<void>(`${this.QR_API}/${id}`).subscribe({
      next: () => {
        this.rules.update(rs => rs.filter(r => r.id !== id));
        this.showToast('Regla eliminada', 'success');
      },
      error: (err) => this.showToast(this.getErrorMessage(err), 'error'),
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


  openValidateModal(): void  { this.showValidateModal.set(true); }
  closeValidateModal(): void { this.showValidateModal.set(false); }

  validateGraph(tipo: 'pdf' | 'csv'): void {
    this.isValidating = true;

    const url = `${this.QR_API}/validate?datasetId=${this.datasetId}&tipo=${tipo}`;

    this.http.get(url, { responseType: 'blob', observe: 'response' }).subscribe({
      next: (res) => {
        const blob = res.body!;
        let filename = 'validation_report';
        const cd = res.headers.get('Content-Disposition');
        if (cd) {
          const match = cd.match(/filename="(.+)"/);
          if (match) filename = match[1];
        } else {
          filename += tipo === 'pdf' ? '.pdf' : '.csv';
        }
        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = filename;
        link.click();
        this.showToast('Validación completada y descargada', 'success');
        this.closeValidateModal();
        this.isValidating = false;
      },
      error: async (err) => {
        let errorMessage: string;
        if (err.error instanceof Blob) {
          const text = await err.error.text();
          try {
            const json = JSON.parse(text);
            errorMessage = json.message || json.error || text;
          } catch {
            errorMessage = text;
          }
        } else {
          errorMessage = this.getErrorMessage(err);
        }
        this.showToast(errorMessage, 'error');
        this.isValidating = false;
      }
    });
  }


  openAbout(): void { this.showAboutModal = true; }
  closeAboutModal(): void { this.showAboutModal = false; }

  showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toast.set({ show: true, message, type });
    setTimeout(() => {
      this.toast.update(t => ({ ...t, show: false }));
    }, 3000);
  }

  private mapToDto(rule: QualityRule) {
    console.log("AAAAAA")
    return {
      content:     rule.content,
      type:        rule.ruleType,
      name:        rule.name,
      description: rule.description,
      datasetId: this.datasetId
    };
  }

  private getErrorMessage(err: any): string {
    if (!err) return 'Error desconocido';
    if (typeof err.error === 'string') return err.error;
    if (err.error?.message) return err.error.message;
    if (err.status) return `Error ${err.status}: ${err.statusText || 'Error en servidor'}`;
    return 'Error inesperado';
  }

  onCsvUpload(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', file);
    formData.append('datasetId', this.datasetId);

    this.http.post<void>(`${this.QR_API}/upload`, formData).subscribe({
      next: () => {
        this.showToast('CSV subido correctamente', 'success');
        this.loadRules();
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
    this.http.get(`${this.QR_API}/export`, {
        params: {
          datasetId: this.datasetId
        },
      responseType: 'blob',
      observe: 'response',
    }).subscribe({
      next: (res) => {
        const blob = res.body!;
        let filename = 'quality_rules.csv';
        const contentDisposition = res.headers.get('Content-Disposition');
        if (contentDisposition) {
          const match = contentDisposition.match(/filename="(.+)"/);
          if (match) filename = match[1];
        }
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
}
