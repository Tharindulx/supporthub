// Run against a disposable demo database: this test creates a ticket and a user.
// Supply PLAYWRIGHT_MODULE when using an existing Playwright installation.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({channel:'chrome',headless:true});
  const context = await browser.newContext({viewport:{width:1440,height:1000}});
  const page = await context.newPage();
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  const out=path.resolve(__dirname,'../playwright-results');fs.mkdirSync(out,{recursive:true});
  async function login(email,password){
    await page.getByLabel('Email address').fill(email);
    await page.getByLabel('Password',{exact:true}).fill(password);
    await page.getByRole('button',{name:/^Sign in/}).click();
    await page.getByRole('heading',{name:'Every request, in good hands.'}).waitFor();
  }
  try {
    await page.goto(process.env.BASE_URL || 'http://localhost:8082');
    await page.getByRole('heading',{name:'Welcome back.'}).waitFor();
    await page.screenshot({path:path.join(out,'01-login.png'),fullPage:true});
    await login('admin@supporthub.test','DemoAdmin!2026');
    await page.getByRole('button',{name:/SH-.*Unable to connect to the office VPN/}).waitFor();
    await page.screenshot({path:path.join(out,'02-workspace.png'),fullPage:true});
    await page.getByRole('button',{name:'New ticket',exact:false}).click();
    await page.getByLabel('Subject',{exact:true}).fill('Browser verification: workstation access');
    await page.getByLabel("What's happening?").fill('Requesting access to the new workstation. Verified through the real Angular interface.');
    await page.getByRole('button',{name:'Create ticket ↗',exact:true}).click();
    const dialog=page.getByRole('dialog');
    await dialog.getByRole('heading',{name:'Browser verification: workstation access'}).waitFor();
    await dialog.getByLabel('Assigned to').selectOption({label:'Jamie Chen'});
    await dialog.getByLabel('Status').selectOption('IN_PROGRESS');
    await dialog.getByRole('button',{name:'Save changes',exact:true}).click();
    await dialog.getByText('Ticket updated.',{exact:true}).waitFor();
    await dialog.getByLabel('Add a comment').fill('Access has been granted. Please confirm you can sign in.');
    await dialog.getByRole('button',{name:'Send comment ↗',exact:true}).click();
    await dialog.getByText('Comment added.',{exact:true}).waitFor();
    await dialog.getByLabel('Status').selectOption('RESOLVED');
    await dialog.getByRole('button',{name:'Save changes',exact:true}).click();
    await dialog.getByText('Ticket updated.',{exact:true}).waitFor();
    await page.screenshot({path:path.join(out,'03-ticket.png'),fullPage:true});
    await page.keyboard.press('Escape');await dialog.waitFor({state:'hidden'});
    await page.getByRole('button',{name:'Team members',exact:false}).click();
    await page.getByRole('heading',{name:/Team directory/}).waitFor();
    await page.getByRole('button',{name:'Sign out',exact:true}).click();
    await login('requester@supporthub.test','DemoUser!2026');
    await page.getByLabel('Search tickets').fill('Browser verification');
    await page.getByRole('button',{name:'Search',exact:true}).click();
    await page.getByRole('heading',{name:'All clear here.'}).waitFor();
    await page.getByLabel('Search tickets').fill('');await page.getByRole('button',{name:'Search',exact:true}).click();
    await page.getByRole('button',{name:/SH-.*Unable to connect to the office VPN/}).waitFor();
    await page.setViewportSize({width:390,height:844});
    await page.screenshot({path:path.join(out,'04-mobile.png'),fullPage:true});
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),'Page overflows mobile viewport');
    await page.getByRole('button',{name:'Sign out',exact:true}).filter({visible:true}).click();
    await page.getByRole('button',{name:'Create an account',exact:true}).click();
    await page.getByLabel('Your name').fill('Browser Test User');
    await page.getByLabel('Email address').fill('browser-'+Date.now()+'@supporthub.test');
    await page.getByLabel('Password',{exact:true}).fill('BrowserTest!2026');
    await page.getByRole('button',{name:'Create account',exact:false}).click();
    await page.getByRole('heading',{name:'Every request, in good hands.'}).waitFor();
    await page.getByRole('heading',{name:'All clear here.'}).waitFor();
    assert.deepEqual(errors,[],'Browser JavaScript errors');
    console.log('PASS: real login, ticket creation, assignment, status, comments, team view, logout, requester isolation, mobile layout and registration. No JavaScript errors.');
  } catch(e) { await page.screenshot({path:path.join(out,'failure.png'),fullPage:true}); console.log(await page.locator('body').innerText()); throw e; } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
