import { Component } from '@angular/core';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-kg-home',
  standalone:true,
  imports: [RouterLink],
  templateUrl: './kg-home.html',
  styleUrl: './kg-home.css',
})
export class KgHome {}
