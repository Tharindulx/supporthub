import { AfterViewInit, Directive, ElementRef, EventEmitter, HostListener, OnDestroy, Output, inject } from '@angular/core';

@Directive({selector:'[appDialogFocus]',standalone:true})
export class DialogFocusDirective implements AfterViewInit,OnDestroy {
  private host:ElementRef<HTMLElement>=inject(ElementRef);
  private previous=document.activeElement as HTMLElement|null;
  @Output() dialogEscape=new EventEmitter<void>();
  private controls(){return Array.from(this.host.nativeElement.querySelectorAll<HTMLElement>('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href]')).filter(el=>el.offsetParent!==null);}
  ngAfterViewInit(){this.controls()[0]?.focus();}
  ngOnDestroy(){this.previous?.focus();}
  @HostListener('document:keydown',['$event']) key(event:KeyboardEvent){
    if(event.key==='Escape'){event.preventDefault();this.dialogEscape.emit();}
    if(event.key!=='Tab')return;
    const controls=this.controls(),first=controls[0],last=controls.at(-1);
    if(!this.host.nativeElement.contains(document.activeElement)){event.preventDefault();first?.focus();return;}
    if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}
    else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}
  }
}
