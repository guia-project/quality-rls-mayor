import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-getkg',
  standalone:true,
  imports: [FormsModule,CommonModule],
  templateUrl: './getkg.html',
  styleUrl: './getkg.css',
})
export class Getkg {
  modo: string = '';
  id: string = '';
  constructor(private router: Router) {}

  buscar() {
    if (this.modo === 'uno') {
      this.router.navigate(['/result-kg', this.id]);
    } else {
      this.router.navigate(['/result-kg']);
    }
}
}
