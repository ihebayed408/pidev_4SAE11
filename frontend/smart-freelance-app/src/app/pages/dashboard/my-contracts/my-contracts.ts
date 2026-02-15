import { Component } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { Card } from '../../../shared/components/card/card';

@Component({
  selector: 'app-my-contracts',
  imports: [Card],
  templateUrl: './my-contracts.html',
  styleUrl: './my-contracts.scss',
  standalone: true,
})
export class MyContracts {
  constructor(public auth: AuthService) {}
}
