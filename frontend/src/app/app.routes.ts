import { Routes } from '@angular/router';
import { QualityRulesComponent }  from './quality-rules-page/quality-rules.component';
import { DatasetsComponent }      from './dataset/datasets.component';
import { DatasetDetailComponent } from './dataset-detail/dataset-detail.component';

export const routes: Routes = [
  { path: '',         redirectTo: 'rules', pathMatch: 'full' },
  { path: 'rules',    component: QualityRulesComponent },
  { path: 'datasets', component: DatasetsComponent },
  { path: 'datasets/:id', component: DatasetDetailComponent },
];
