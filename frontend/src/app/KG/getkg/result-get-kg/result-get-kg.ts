import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-result-get-kg',
  standalone:true,
  imports: [CommonModule],
  templateUrl: './result-get-kg.html',
  styleUrl: './result-get-kg.css',
})
export class ResultGetKg implements OnInit{
  id: string | null = null;
  result: any = undefined;
  isError = false;

  constructor(private route: ActivatedRoute, private http: HttpClient, private cdr: ChangeDetectorRef) {}

  isArray(value: any): boolean {
    return Array.isArray(value);
  }

  ngOnInit() {
    this.id = this.route.snapshot.paramMap.get('id');

    if (this.id) {
      this.http.get(`http://localhost:8080/kg/${this.id}`)
        .subscribe({
          next:res => {
          this.result = res;
          this.isError=false;
          this.cdr.detectChanges();
        },
        error:(err)=>{
          this.result = err.error || err.message || 'Error desconocido';
          this.isError = true;
          this.cdr.detectChanges();
        }
        });
    } else {
      this.http.get(`http://localhost:8080/kg`)
        .subscribe(res => {
          this.result = res;
          this.cdr.detectChanges();
        });
    }
  }}
