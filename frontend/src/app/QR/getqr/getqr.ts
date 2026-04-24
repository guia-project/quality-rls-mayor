import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';


@Component({
  selector: 'app-getqr',
  standalone:true,
  imports: [FormsModule,CommonModule],
  templateUrl: './getqr.html',
  styleUrl: './getqr.css',
})
export class Getqr {
  modo: string = '';
  id: string = '';
  constructor(private router: Router) {}

  buscar() {
    if (this.modo === 'uno') {
      this.router.navigate(['/result-qr', this.id]);
    } else {
      this.router.navigate(['/result-qr']);
    }
}
}

