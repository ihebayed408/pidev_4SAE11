import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-home',
  imports: [RouterLink, Card],
  templateUrl: './home.html',
  styleUrl: './home.scss',
  standalone: true,
})
export class Home {}
