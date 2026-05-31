import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ViewEncapsulation } from '@angular/core';

export interface Dataset {
  id: string;
  name: string;
  endpointUrl: string;
  rules?: any[];
}

@Component({
  selector: 'app-datasets',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './datasets.component.html',
  styleUrls: ['../quality-rules-page/quality-rules.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class DatasetsComponent implements OnInit {
  private readonly API = '/api/kg';

  datasets  = signal<Dataset[]>([]);
  isLoading = false;
  showAboutModal = false;

  toast = signal({ show: false, message: '', type: 'info' as 'success' | 'error' | 'info' });

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.loadDatasets();
  }

  private loadDatasets(): void {
    this.isLoading = true;
    this.http.get<Dataset[]>(this.API).subscribe({
      next: (data) => {
        this.datasets.set(data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error cargando datasets', err);
        this.showToast(this.getErrorMessage(err), 'error');
        this.isLoading = false;
      }
    });
  }

  openDataset(ds: Dataset): void {
    this.router.navigate(['/datasets', ds.id]);
  }

  goToRules(): void {
    this.router.navigate(['/rules']);
  }

  openAbout(): void { this.showAboutModal = true; }
  closeAboutModal(): void { this.showAboutModal = false; }

  showToast(message: string, type: 'success' | 'error' | 'info'): void {
    this.toast.set({ show: true, message, type });
    setTimeout(() => {
      this.toast.update(t => ({ ...t, show: false }));
    }, 3000);
  }

  private getErrorMessage(err: any): string {
    if (!err) return 'Error desconocido';
    if (typeof err.error === 'string') return err.error;
    if (err.error?.message) return err.error.message;
    if (err.status) return `Error ${err.status}: ${err.statusText || 'Error en servidor'}`;
    return 'Error inesperado';
  }

  deleteDataset(ds: Dataset, event: MouseEvent): void {
    event.stopPropagation();
    if (!confirm(`¿Eliminar el dataset "${ds.name}"?`)) {
      return;
    }
    this.http.delete(`${this.API}/${ds.id}`).subscribe({
      next: () => {
        this.datasets.update(datasets =>
          datasets.filter(d => d.id !== ds.id)
        );
        this.showToast('Dataset eliminado correctamente', 'success');
      },
      error: (err) => {
        console.error('Error eliminando dataset', err);
        this.showToast(this.getErrorMessage(err), 'error');
      }
    });
  }
}
